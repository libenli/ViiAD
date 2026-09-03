from __future__ import annotations

import datetime as _dt
import html
import shutil
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REFERENCE_DOCX = Path(r"F:\MySher\2026\文档\软著登记 - 麦哲ViiAD百万大屏广告平台V1.1 - Berry\说明书.docx")
OUTPUT_DIR = ROOT / "output" / "doc"
OUTPUT_DOCX = OUTPUT_DIR / "ViiAD百万大屏广告平台V1.1_软件说明书_初稿.docx"


def esc(text: object) -> str:
    return html.escape(str(text), quote=False)


def run(text: str, *, bold: bool = False, size: int = 21, color: str | None = None) -> str:
    props = [
        '<w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体"/>',
        f'<w:sz w:val="{size}"/>',
    ]
    if bold:
        props.append("<w:b/>")
    if color:
        props.append(f'<w:color w:val="{color}"/>')
    safe_text = esc(text)
    return f"<w:r><w:rPr>{''.join(props)}</w:rPr><w:t>{safe_text}</w:t></w:r>"


def paragraph(
    text: str = "",
    *,
    style: str | None = None,
    align: str | None = None,
    bold: bool = False,
    size: int = 21,
    color: str | None = None,
    before: int = 0,
    after: int = 120,
) -> str:
    ppr = []
    if style:
        ppr.append(f'<w:pStyle w:val="{style}"/>')
    if align:
        ppr.append(f'<w:jc w:val="{align}"/>')
    ppr.append(f'<w:spacing w:before="{before}" w:after="{after}" w:line="360" w:lineRule="auto"/>')
    return f"<w:p><w:pPr>{''.join(ppr)}</w:pPr>{run(text, bold=bold, size=size, color=color)}</w:p>"


def page_break() -> str:
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


def heading1(text: str) -> str:
    return paragraph(text, style="2", bold=True, size=28, before=240, after=180)


def heading2(text: str) -> str:
    return paragraph(text, style="3", bold=True, size=24, before=180, after=120)


def normal(text: str) -> str:
    return paragraph(text, size=21, after=120)


def bullet(text: str) -> str:
    return paragraph(f"（{text}）", size=21, after=80)


def cell(text: str, width: int = 3000, *, shade: str | None = None, bold: bool = False) -> str:
    tcpr = [f'<w:tcW w:w="{width}" w:type="dxa"/>']
    if shade:
        tcpr.append(f'<w:shd w:fill="{shade}"/>')
    return (
        "<w:tc>"
        f"<w:tcPr>{''.join(tcpr)}</w:tcPr>"
        f"{paragraph(text, bold=bold, size=20, after=60)}"
        "</w:tc>"
    )


def table(rows: list[list[str]], *, widths: list[int] | None = None, header: bool = True) -> str:
    if not rows:
        return ""
    col_count = max(len(row) for row in rows)
    widths = widths or [int(9072 / col_count)] * col_count
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in widths[:col_count])
    body = [
        "<w:tbl>",
        (
            "<w:tblPr>"
            '<w:tblW w:w="9072" w:type="dxa"/>'
            '<w:tblBorders>'
            '<w:top w:val="single" w:sz="4" w:color="BFBFBF"/>'
            '<w:left w:val="single" w:sz="4" w:color="BFBFBF"/>'
            '<w:bottom w:val="single" w:sz="4" w:color="BFBFBF"/>'
            '<w:right w:val="single" w:sz="4" w:color="BFBFBF"/>'
            '<w:insideH w:val="single" w:sz="4" w:color="D9D9D9"/>'
            '<w:insideV w:val="single" w:sz="4" w:color="D9D9D9"/>'
            "</w:tblBorders>"
            "</w:tblPr>"
        ),
        f"<w:tblGrid>{grid}</w:tblGrid>",
    ]
    for ri, row in enumerate(rows):
        body.append("<w:tr>")
        for ci in range(col_count):
            body.append(cell(row[ci] if ci < len(row) else "", widths[ci], shade="F2F2F2" if header and ri == 0 else None, bold=header and ri == 0))
        body.append("</w:tr>")
    body.append("</w:tbl>")
    return "".join(body)


def screenshot(label: str, caption: str) -> str:
    return (
        "<w:tbl>"
        "<w:tblPr>"
        '<w:tblW w:w="9072" w:type="dxa"/>'
        '<w:tblBorders>'
        '<w:top w:val="single" w:sz="8" w:color="808080"/>'
        '<w:left w:val="single" w:sz="8" w:color="808080"/>'
        '<w:bottom w:val="single" w:sz="8" w:color="808080"/>'
        '<w:right w:val="single" w:sz="8" w:color="808080"/>'
        "</w:tblBorders>"
        "</w:tblPr>"
        '<w:tblGrid><w:gridCol w:w="9072"/></w:tblGrid>'
        '<w:tr><w:trPr><w:trHeight w:val="3600" w:hRule="atLeast"/></w:trPr>'
        '<w:tc><w:tcPr><w:tcW w:w="9072" w:type="dxa"/><w:vAlign w:val="center"/><w:shd w:fill="F7F7F7"/></w:tcPr>'
        f'{paragraph("【截图占位】" + label, align="center", bold=True, size=24, color="666666", after=120)}'
        f'{paragraph("后续替换为实际系统截图", align="center", size=20, color="999999", after=60)}'
        "</w:tc></w:tr></w:tbl>"
        f'{paragraph(caption, align="center", size=19, after=180)}'
    )


def toc_item(title: str, page: str = "") -> str:
    dots = " ................................................................ "
    return paragraph(f"{title}{dots}{page}", size=21, after=40)


def build_cover() -> list[str]:
    today = _dt.date.today().strftime("%Y年%m月%d日")
    return [
        paragraph("ViiAD百万大屏广告平台", align="center", bold=True, size=36, before=1800, after=220),
        paragraph("软件说明书", align="center", bold=True, size=32, after=1200),
        paragraph("软件版本：V1.1", align="center", size=22, after=120),
        paragraph("文档版本：初稿", align="center", size=22, after=120),
        paragraph(f"编制日期：{today}", align="center", size=22, after=900),
        paragraph("北京麦哲科技有限公司", align="center", size=22, after=80),
        page_break(),
    ]


def build_toc() -> list[str]:
    items = [
        "软件介绍",
        "系统要求",
        "系统部署说明",
        "运行系统",
        "登录",
        "工作台",
        "用户管理",
        "权限管理",
        "设备管理",
        "代理商管理",
        "广告主管理",
        "广告管理",
        "素材管理",
        "投放计划",
        "下发记录",
        "工单反馈",
        "数据报表",
        "账单结算",
        "操作日志",
        "常见问题",
    ]
    parts = [paragraph("目录", align="center", bold=True, size=30, before=240, after=360)]
    parts.extend(toc_item(item) for item in items)
    parts.append(page_break())
    return parts


def build_intro() -> list[str]:
    return [
        heading1("软件介绍"),
        normal("ViiAD百万大屏广告平台是一套面向大屏广告投放、素材管理、投放计划、设备下发、播放数据统计和财务结算的 Web 管理系统。系统围绕广告投放主链路进行设计，支持从广告创建、素材上传审核、投放计划编排、设备选择、下发执行、播放日志回收、数据报表分析到账单结算的业务闭环。"),
        normal("平台适用于商场、楼宇、社区、交通场站、园区、校园等大屏广告运营场景，可帮助运营方统一管理广告主、代理商、广告内容、投放设备及投放效果数据。"),
        normal("本说明书用于描述 ViiAD 百万大屏广告平台 V1.1 的功能组成、运行环境、部署方式和主要页面操作方法，可作为软件著作权登记材料及系统交付说明材料使用。"),
        screenshot("软件整体入口与系统首页", "图 1 软件首页截图占位"),
    ]


def build_requirements() -> list[str]:
    rows = [
        ["组件", "推荐配置", "最低配置"],
        ["服务器 CPU", "4 核及以上", "2 核"],
        ["服务器内存", "8GB 及以上", "4GB"],
        ["数据库", "MySQL 5.x", "兼容 MySQL 5.x 的数据库版本"],
        ["缓存服务", "Redis 5.x 及以上", "Redis 4.x 及以上"],
        ["运行环境", "JDK 8", "JDK 8"],
        ["Web 服务器", "Nginx 1.18 及以上", "支持静态资源部署和反向代理"],
        ["浏览器", "Chrome / Edge 最新稳定版", "支持 HTML5 的现代浏览器"],
        ["显示分辨率", "1920 x 1080 及以上", "1366 x 768"],
    ]
    return [
        heading1("系统要求"),
        normal("为确保系统稳定运行，建议在正式部署前准备满足以下条件的服务器、数据库和浏览器环境。"),
        table(rows, widths=[2200, 3600, 3272]),
    ]


def build_deploy() -> list[str]:
    return [
        heading1("系统部署说明"),
        normal("ViiAD 百万大屏广告平台采用前后端分离架构。前端使用 Vue 3、Vite、TypeScript、Element Plus 构建，打包后由 Nginx 提供静态资源访问；后端使用 Java 8、Spring Boot、MyBatis-Plus 构建，打包为 Jar 服务运行；数据库采用 MySQL 5.x；缓存服务采用 Redis；素材文件支持上传至 OSS。"),
        heading2("后端部署"),
        normal("后端服务打包完成后，将 Jar 文件和对应配置放置到服务器目录。测试环境可使用 test profile，生产环境可使用 prod profile。启动时通过 spring.profiles.active 指定运行环境。"),
        normal("启动命令示例：java -jar ad-system.jar --spring.profiles.active=test"),
        heading2("前端部署"),
        normal("前端执行构建命令后生成 dist 目录，将 dist 目录内容上传至 Nginx 静态资源目录。Nginx 负责访问前端页面，并将 /api 请求反向代理到后端服务。"),
        normal("测试环境构建命令示例：npm run build:test。生产环境构建命令示例：npm run build:prod。"),
        heading2("素材上传配置"),
        normal("系统素材上传支持图片、视频、HTML、ZIP 等文件类型。测试环境和生产环境建议使用 OSS 存储，上传成功后后端返回文件访问地址，前端自动回填到素材表单，供广告投放和大屏播放使用。"),
        screenshot("Nginx 与后端服务部署结构", "图 2 系统部署结构截图占位"),
    ]


def build_run() -> list[str]:
    return [
        heading1("运行系统"),
        normal("系统部署完成后，用户通过浏览器访问平台地址进入登录页面。后端服务正常启动、数据库连接成功、Nginx 反向代理配置正确时，用户可完成登录并进入工作台。"),
        normal("运行检查包括：后端端口是否监听、数据库是否可连接、Redis 是否可连接、OSS 上传配置是否有效、前端页面是否能正常访问、接口请求是否返回正常。"),
        screenshot("系统访问地址与登录入口", "图 3 系统运行入口截图占位"),
    ]


def build_login() -> list[str]:
    return [
        heading1("登录"),
        normal("当前发布版本登录模块仅支持密码登录。用户进入登录页后，输入邮箱账号、密码和图片验证码，点击“登录”按钮，系统校验通过后进入后台管理首页。"),
        normal("默认账号示例为 admin@mysher.com，初始密码由系统管理员分配。不同账号登录后，系统会根据其角色权限展示不同菜单和按钮。"),
        normal("若账号、密码或验证码填写错误，系统会在登录页给出错误提示；若账号被禁用或无后台访问权限，系统将拒绝登录。"),
        screenshot("登录页 - 密码登录", "图 4 登录页截图占位"),
    ]


def build_dashboard() -> list[str]:
    return [
        heading1("工作台"),
        normal("工作台用于展示平台运营总览，包括广告投放概况、素材审核状态、设备在线状态、播放趋势、工单提醒和结算概况等内容。用户登录后可通过工作台快速掌握系统当前运行情况。"),
        normal("工作台页面采用卡片和图表结合的方式展示关键指标，便于管理人员查看今日播放次数、曝光量、投放中计划数、异常设备数以及待处理工单数量。"),
        screenshot("工作台数据驾驶舱", "图 5 工作台截图占位"),
    ]


def build_user_permission() -> list[str]:
    role_rows = [
        ["序号", "角色", "账号示例", "主要权限"],
        ["1", "超级管理员", "admin@mysher.com", "全量菜单、用户管理、权限管理、业务管理"],
        ["2", "开发工程师", "developer@mysher.com", "系统配置、设备、下发、日志查看"],
        ["3", "运维工程师", "operator@mysher.com", "设备维护、下发记录、工单处理"],
        ["4", "区域管理员", "region@mysher.com", "区域广告、区域设备、区域报表"],
        ["5", "业务运营", "business@mysher.com", "广告、素材、计划、代理商、广告主"],
        ["6", "广告内容审核员", "reviewer@mysher.com", "广告审核、素材审核"],
        ["7", "财务", "finance@mysher.com", "账单结算、财务报表"],
        ["8", "数据分析师", "analyst@mysher.com", "数据报表、播放日志"],
        ["9", "广告代理商", "agent01@mysher.com", "代理商范围内业务数据"],
        ["10", "广告主", "advertiser01@mysher.com", "广告主范围内广告与报表"],
        ["11", "客服", "service@mysher.com", "工单反馈处理"],
        ["12", "审计师", "auditor@mysher.com", "业务数据只读、操作日志"],
        ["13", "受众账号", "audience@mysher.com", "受限入口和预留功能"],
    ]
    return [
        heading1("用户管理"),
        normal("用户管理用于维护平台后台账号，包括账号新增、编辑、启用、禁用、重置密码和角色分配等操作。系统账号以邮箱形式作为登录标识，便于统一管理和后续接入通知能力。"),
        normal("管理员可根据岗位职责为用户分配一个或多个角色。当前发布版本登录后直接依据用户权限展示可访问的菜单和按钮。"),
        table(role_rows, widths=[700, 1700, 2500, 4172]),
        screenshot("用户管理列表", "图 6 用户管理截图占位"),
        heading1("权限管理"),
        normal("权限管理用于配置角色与菜单、按钮权限之间的关系。系统通过角色权限控制用户可访问的功能范围，通过数据权限控制用户可查看或操作的数据范围。"),
        normal("超级管理员可进入权限管理页面，为角色勾选菜单权限和操作权限。权限保存后，对应角色用户重新登录或刷新权限后生效。"),
        screenshot("权限管理页面", "图 7 权限管理截图占位"),
    ]


def build_device() -> list[str]:
    return [
        heading1("设备管理"),
        normal("设备管理用于维护大屏终端设备信息，包括设备编号、设备名称、所属区域、所属楼宇、屏幕规格、在线状态、故障状态和当前投放计划等内容。"),
        normal("运营或运维人员可在设备列表中按区域、状态、设备编号等条件筛选设备，并查看设备详情。投放计划启动后，被选中的在线设备会关联当前投放计划；计划暂停或结束后，对应设备的当前计划信息会释放。"),
        screenshot("设备管理列表", "图 8 设备管理列表截图占位"),
        screenshot("设备详情", "图 9 设备详情截图占位"),
    ]


def build_partners() -> list[str]:
    return [
        heading1("代理商管理"),
        normal("代理商管理用于维护广告代理商主体信息，包括代理商名称、联系人、联系方式、结算方式、状态等。代理商账号登录后仅能查看其数据范围内的广告、素材、计划、报表和账单信息。"),
        screenshot("代理商管理列表", "图 10 代理商管理截图占位"),
        heading1("广告主管理"),
        normal("广告主管理用于维护广告主主体信息，包括广告主名称、行业类型、联系人、联系方式、状态等。广告主账号登录后仅能查看本广告主相关的广告投放和报表数据。"),
        screenshot("广告主管理列表", "图 11 广告主管理截图占位"),
    ]


def build_ad_material_plan() -> list[str]:
    return [
        heading1("广告管理"),
        normal("广告管理是平台业务主流程的起点。业务运营人员可创建广告，填写广告名称、广告主、代理商、广告类型、投放目标、投放区域、预算金额和广告说明。广告保存后状态为草稿，可继续编辑或提交审核。"),
        normal("广告提交审核后，审核人员可对广告内容进行审核，通过后广告状态变为已通过，后续可关联素材和投放计划。若审核不通过，系统记录审核意见并返回业务运营人员修改。"),
        screenshot("广告列表", "图 12 广告列表截图占位"),
        screenshot("新建广告表单", "图 13 新建广告表单截图占位"),
        screenshot("广告详情", "图 14 广告详情截图占位"),
        heading1("素材管理"),
        normal("素材管理用于上传、维护和审核广告素材。当前系统支持图片、视频、HTML、ZIP 等素材文件上传，素材上传到 OSS 后自动生成访问地址，并回填素材表单。"),
        normal("素材保存后可提交审核。审核通过的素材可被投放计划选择，未审核或审核未通过的素材不能进入正式投放流程。素材列表中将尺寸和时长分开显示，方便快速识别素材规格。"),
        screenshot("素材管理列表", "图 15 素材管理列表截图占位"),
        screenshot("素材上传表单", "图 16 素材上传表单截图占位"),
        screenshot("素材详情", "图 17 素材详情截图占位"),
        heading1("投放计划"),
        normal("投放计划用于将已审核广告、已审核素材与可用设备进行关联，并设置投放时间、投放时段和排期状态。用户创建计划时需先选择广告，再加载该广告下已审核通过的素材，并从设备列表中选择可用设备。"),
        normal("计划保存后可提交排期，排期通过后可启动投放。计划启动时系统生成下发记录，在线设备生成成功记录，离线或异常设备生成失败记录，并可联动生成工单。"),
        screenshot("投放计划列表", "图 18 投放计划列表截图占位"),
        screenshot("新建投放计划", "图 19 新建投放计划截图占位"),
        screenshot("投放计划详情", "图 20 投放计划详情截图占位"),
    ]


def build_delivery_workorder_report_bill_log() -> list[str]:
    return [
        heading1("下发记录"),
        normal("下发记录用于展示投放计划与设备之间的执行结果。计划启动后，系统按设备生成下发记录，记录计划编号、设备编号、素材信息、下发状态、失败原因和下发时间。"),
        normal("对于下发失败的记录，用户可查看失败原因并执行重试。下发成功后，系统可生成播放日志，为后续数据报表和账单结算提供基础数据。"),
        screenshot("下发记录列表", "图 21 下发记录截图占位"),
        heading1("工单反馈"),
        normal("工单反馈用于处理投放执行或设备运行过程中的异常问题。系统可在下发失败时自动生成高优先级工单，客服或运维人员也可手动创建工单。"),
        normal("工单状态支持打开、已分配、处理中、已关闭等流转。处理人员可在工单中填写处理意见，便于后续追踪问题闭环。"),
        screenshot("工单反馈列表", "图 22 工单反馈截图占位"),
        heading1("数据报表"),
        normal("数据报表用于统计广告投放效果和设备播放情况。用户可按广告、计划、设备、区域和日期范围筛选数据，查看播放次数、曝光量、播放完成率、活跃设备数和播放时长等指标。"),
        normal("报表中心同时提供播放日志明细，便于运营人员核查具体设备、具体计划的播放记录。"),
        screenshot("数据报表页面", "图 23 数据报表截图占位"),
        heading1("账单结算"),
        normal("账单结算用于管理广告主和代理商账单。财务人员可查看账单列表、账单金额、确认状态和支付状态，并对待确认账单执行确认，对已确认账单执行支付登记。"),
        normal("账单数据可基于广告投放计划和播放报表形成结算依据。非财务角色无法看到或执行账单确认、支付等敏感操作。"),
        screenshot("账单结算列表", "图 24 账单结算截图占位"),
        heading1("操作日志"),
        normal("操作日志用于记录用户在系统中的关键操作，包括登录、创建、编辑、删除、审核、启动投放、下发重试、账单确认等行为。审计人员可通过操作日志追踪操作人、操作时间、操作模块和操作结果。"),
        screenshot("操作日志列表", "图 25 操作日志截图占位"),
    ]


def build_faq() -> list[str]:
    rows = [
        ["问题", "处理方式"],
        ["无法登录系统", "检查账号、密码、验证码是否正确，并确认账号是否启用。"],
        ["素材上传失败", "检查文件类型、文件大小、OSS 配置和网络连接。"],
        ["投放计划无法选择素材", "确认广告已审核通过，且该广告下存在已审核通过的素材。"],
        ["投放计划无法选择设备", "确认设备状态可用，且设备未处于不可投放或故障状态。"],
        ["下发记录失败", "查看失败原因，检查设备在线状态后执行重试。"],
        ["报表无数据", "确认计划已启动且存在成功下发和播放日志。"],
    ]
    return [
        heading1("常见问题"),
        normal("系统使用过程中如遇到异常，可根据以下说明进行初步排查。若问题无法解决，可联系系统管理员或技术支持人员处理。"),
        table(rows, widths=[3000, 6072]),
    ]


def build_document_xml() -> str:
    parts: list[str] = []
    for builder in [
        build_cover,
        build_toc,
        build_intro,
        build_requirements,
        build_deploy,
        build_run,
        build_login,
        build_dashboard,
        build_user_permission,
        build_device,
        build_partners,
        build_ad_material_plan,
        build_delivery_workorder_report_bill_log,
        build_faq,
    ]:
        parts.extend(builder())
    body = "".join(parts)
    sect_pr = (
        "<w:sectPr>"
        '<w:pgSz w:w="11900" w:h="16838"/>'
        '<w:pgMar w:top="1440" w:right="1440" w:bottom="1004" w:left="1440" w:header="0" w:footer="0" w:gutter="0"/>'
        "</w:sectPr>"
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" '
        'xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" '
        'xmlns:o="urn:schemas-microsoft-com:office:office" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
        'xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" '
        'xmlns:v="urn:schemas-microsoft-com:vml" '
        'xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" '
        'xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" '
        'xmlns:w10="urn:schemas-microsoft-com:office:word" '
        'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
        'xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" '
        'xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" '
        'xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" '
        'xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" '
        'xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" '
        'mc:Ignorable="w14 wp14">'
        f"<w:body>{body}{sect_pr}</w:body></w:document>"
    )


def main() -> None:
    if not REFERENCE_DOCX.exists():
        raise FileNotFoundError(f"Reference document not found: {REFERENCE_DOCX}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    temp_docx = OUTPUT_DOCX.with_suffix(".tmp.docx")
    document_xml = build_document_xml().encode("utf-8")
    with zipfile.ZipFile(REFERENCE_DOCX, "r") as zin, zipfile.ZipFile(temp_docx, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename == "word/document.xml":
                zout.writestr(item, document_xml)
            else:
                zout.writestr(item, zin.read(item.filename))
    if OUTPUT_DOCX.exists():
        OUTPUT_DOCX.unlink()
    shutil.move(str(temp_docx), str(OUTPUT_DOCX))
    print(OUTPUT_DOCX)


if __name__ == "__main__":
    main()
