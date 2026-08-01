import os
import sys
import time
import re
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup
import markdownify
import requests

# 兼容 Windows 控制台编码，防止乱码或错误
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_URL = "https://bot.q.qq.com"
START_URLS = [
    "https://bot.q.qq.com/wiki/develop/api-v2/dev-prepare/getting-started.html",
    "https://bot.q.qq.com/wiki/agent-qqbot/",
]
OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "qq_bot_docs")

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}

def clean_url(url):
    """去除 URL 的锚点及查询参数，处理尾部斜杠"""
    url = url.split("#")[0].split("?")[0]
    if url.endswith("/") and len(url) > 10:
        url = url[:-1]
    return url

def extract_wiki_links_from_text(text):
    """使用正则从 HTML 或 JS 源码中全面提取所有 /wiki/ 文档路径"""
    links = set()
    matches = re.findall(r'["\'](/wiki/[a-zA-Z0-9_\-\/\.]+(?:\.html|/))["\']', text)
    for path in matches:
        full_url = urljoin(BASE_URL, path)
        clean = clean_url(full_url)
        if clean and not clean.endswith((".png", ".jpg", ".jpeg", ".gif", ".zip", ".pdf", ".js", ".css")):
            links.add(clean)
    return links

def get_all_page_links(url, soup, html_text, fetched_js):
    """多维度提取文档链接：DOM a标签 + 源码正则 + VuePress路由JS包"""
    links = set()
    
    # 1. DOM a标签
    for a in soup.select("a[href]"):
        href = a.get("href")
        if not href or href.startswith("javascript:") or href.startswith("mailto:"):
            continue
        full_url = urljoin(BASE_URL, href)
        parsed = urlparse(full_url)
        if parsed.netloc == "bot.q.qq.com" and "/wiki/" in parsed.path:
            clean = clean_url(full_url)
            if clean and not clean.endswith((".png", ".jpg", ".jpeg", ".gif", ".zip", ".pdf")):
                links.add(clean)
                
    # 2. HTML 源码文本正则匹配
    links.update(extract_wiki_links_from_text(html_text))
    
    # 3. 首次抓取时，扫描页面引用的核心 JS 文件（VuePress 把全站页面列表打包在 js 中）
    for script in soup.select("script[src]"):
        src = script.get("src")
        if src and ("app." in src or "/js/" in src) and src not in fetched_js:
            fetched_js.add(src)
            js_url = urljoin(BASE_URL, src)
            try:
                js_resp = requests.get(js_url, headers=headers, timeout=5)
                js_resp.encoding = "utf-8"
                if js_resp.status_code == 200:
                    js_links = extract_wiki_links_from_text(js_resp.text)
                    links.update(js_links)
            except Exception:
                pass
                
    return links

def crawl():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    visited = set()
    queue = list(START_URLS)
    fetched_js = set()
    
    print(f"[{time.strftime('%X')}] 准备启动抓取，输出目录: {OUTPUT_DIR}")
    
    success_count = 0
    while queue:
        url = clean_url(queue.pop(0))
        if url in visited:
            continue
        visited.add(url)
        
        print(f"[{time.strftime('%X')}] 正在下载 ({len(visited)}): {url}")
        try:
            resp = requests.get(url, headers=headers, timeout=10)
            resp.encoding = "utf-8"
            if resp.status_code != 200:
                print(f"  -> 失败: HTTP 状态码 {resp.status_code}")
                continue
                
            soup = BeautifulSoup(resp.text, "html.parser")
            
            # 自动发现全站文档链接
            new_links = get_all_page_links(url, soup, resp.text, fetched_js)
            for link in sorted(new_links):
                if link not in visited and link not in queue:
                    queue.append(link)
            
            # 定位主内容区域
            main_content = soup.select_one("main.page")
            if not main_content:
                print("  -> 跳过: 未发现 main.page 内容区域")
                continue
                
            # 移除翻页、页脚更新时间、标题右边的 # 锚点符号
            for tag in main_content.select(".page-edit, .page-nav, .header-anchor"):
                tag.decompose()
                
            # HTML 转 Markdown
            md_text = markdownify.markdownify(
                str(main_content), 
                heading_style="ATX", 
                strip=["script", "style"]
            ).strip()
            
            # 构造相对本地的文件保存路径
            parsed_path = urlparse(url).path
            rel_path = parsed_path.lstrip("/")
            if rel_path.startswith("wiki/"):
                rel_path = rel_path[5:]
            if not rel_path or rel_path.endswith("/"):
                rel_path += "index.html"
            elif not rel_path.endswith(".html"):
                rel_path += "/index.html"
                
            file_path = os.path.join(OUTPUT_DIR, rel_path.replace(".html", ".md"))
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            
            # 在 Markdown 前面添加页面标题元信息
            title_tag = soup.select_one("title")
            title = title_tag.text.strip() if title_tag else url
            header_md = f"# {title}\n\n> 原始链接: [{url}]({url})\n\n---\n\n"
            
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(header_md + md_text)
                
            success_count += 1
            print(f"  -> 已保存: {rel_path.replace('.html', '.md')}")
            
            time.sleep(0.25)
        except Exception as e:
            print(f"  -> 发生异常: {e}")
            
    print(f"\n[OK] 抓取完成！共成功保存了 {success_count} 个 Markdown 页面至：{OUTPUT_DIR}")

if __name__ == "__main__":
    crawl()
