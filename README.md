# Daedalus

基于 [iTXTech/Daedalus](https://github.com/iTXTech/Daedalus) 的个人分支：重写了 DNS 转发层，并补上一批面向内网、自建 DNS 场景的功能。上游的原始说明折叠在文末。

## 相对上游的新增能力

### 转发
- **协议按服务器自动选择**：853 端口走 DoT，带路径的地址走 DoH，其余按设置走 UDP 或 TCP。首选 DoT、备用 DoH 可以混用，不再受全局"查询方式"限制。
- **应用内故障转移**：每个查询先问首选，2 秒无果改问备用，整条链在 4.5 秒内必有结果或返回 SERVFAIL，不再依赖系统解析器的超时和"服务器不可用"记忆。
- **DoT / TCP 长连接复用**；查询 socket 绑定到当前实际使用的网络，切网瞬间不会走错口；用域名配置的 DoT 带 SNI 并校验证书主机名。

### 网络规则
- 按**所在网段**（支持多个 CIDR）或**移动数据**自动选择首选/备用服务器，切网即时生效，不重建 VPN；都不匹配时使用设置里的默认首选/备用。
- 典型用法：家里 WiFi 网段 → 内网 SmartDNS（DoT 853）；其他网络 → 公网 DoH。

### 服务器
- 自定义服务器可**经 SOCKS5 代理连接**，例如 EasyTier 非 TUN 模式暴露的代理，用来访问虚拟网络内的 DNS。代理只承载 TCP，这类服务器的 UDP 查询自动改走 TCP。
- 自定义服务器可**绑定 TLS 证书**（自签名或私有 CA，例如 SmartDNS 自动生成的 `smartdns-cert.pem`），只对这台服务器生效，其余服务器仍只信任系统 CA。
- 内置服务器可在 设置 → 服务器管理 里逐个关闭；自定义服务器排在列表顶部，最新添加的最上。

### 可观测
- **查询记录**页：每条查询的域名、类型、命中的规则、回答的服务器与协议、结果 IP、耗时；首选失败转移到备选时附上失败原因。按域名过滤，自动刷新，保留最近 300 条。
- 测试页：停止按钮、正确的连接和读超时、结果标注实际使用的协议。

### 配置
- 配置改存内部存储、临时文件加原子重命名写入，解析失败保留坏文件，修复了自定义服务器偶发丢失的问题；增删改后立即保存并被运行中的 VPN 采用。
- 设置 → 备份与恢复：**导出 / 导入 / 从 URL 导入**，一个 JSON 文件覆盖自定义服务器（含证书）、默认首选备用、网络规则、过滤规则、应用列表和全部设置。导入为合并更新，不删除已有内容；服务器以 `builtin:<地址>` / `custom:<地址>:<端口>` 引用，换机可用。

### 其他
- 主题：跟随系统 / 暗色 / 亮色。每个设置项都带用途说明。
- 修复：文件选择器返回时误启动 VPN；非高级模式下选 DoH 服务器无法启动；TCP 响应短读；测试页额外服务器解析错误。

## 使用要点
- 以上能力都需要打开 **设置 → 高级系统设置 → 开启**。关闭时应用只是把服务器 IP 交给系统做普通 DNS，端口、加密、代理、证书和规则都不生效。
- 走 SOCKS5 代理的 DNS 服务器需要开放 TCP 53（SmartDNS 加 `bind-tcp [::]:53`）或改用 853。
- 内网 DNS 请用 IP 配置。VPN 启动和切网时会先在底层网络上解析服务器域名，不会绕回 VPN 自身。

## 构建
```
./gradlew assemblePureDebug
```
需要 JDK 11 以上（CI 使用 17），依赖经阿里云镜像拉取。推送到 GitHub 会自动触发 Actions 构建并上传 APK 产物，也可以在 Actions 页手动运行。

<details>
<summary>这里是原始内容</summary>

# iTXTech Daedalus

__No root required Android DNS modifier and Hosts/DNSMasq resolver.__

## Installations
* __[Releases](https://github.com/iTXTech/Daedalus/releases)__ - Release signature
* __[Play Test](https://play.google.com/apps/testing/org.itxtech.daedalus)__ - Release signature

[<img alt='Get it on Google Play'
      src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'
      height="80">](https://play.google.com/store/apps/details?id=org.itxtech.daedalus)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/org.itxtech.daedalus)

## Useful links
* __[Telegram](https://t.me/iTXTechDaedalus)__ - Join chat
* __[Wiki](https://github.com/iTXTech/Daedalus/wiki)__ - Pending update

## Introduction

This application creates a VPN tunnel to handle all DNS requests.<br>
<br>
Features:
* No root access required, no ads contained
* Functional under data connection
* A tester for DNS servers
* IPv6 support (including Rules!)
* Custom DNS server
* Custom hosts and DNSMasq configuration
* EXTREME LOW power consume
* Material Design

Supported DNS Query Methods:
* UDP
* TCP 
* DNS over TLS ([RFC7858](https://tools.ietf.org/html/rfc7858))
* DNS over HTTPS ([RFC8484](https://tools.ietf.org/html/rfc8484))
* DNS over HTTPS ([Google JSON](https://developers.google.com/speed/public-dns/docs/dns-over-https))
<br>

__Users must comply with local laws and regulations.__<br>

## DNS Server Providers

* __CuteDNS__ - *Shutdown according to regulations*
* __[FUN DNS](http://fundns.cn)__ - *Shutdown according to regulations*
* __[Pure DNS](https://puredns.cn/)__ - *Shutdown according to regulations*
* __[PdoMo-DNS](https://pdomo.me/)__ - *Shutdown according to regulations*
* __[rubyfish](https://www.rubyfish.cn)__ - *Free DoT/DoH DNS*

## Rule Providers

* __[hosts](https://github.com/googlehosts/hosts)__ by *[googlehosts](https://github.com/googlehosts)* - [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.zh)
* __[yhosts](https://github.com/vokins/yhosts)__ by *[vokins](https://github.com/vokins)* - [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/)

## Requirements

* Minimum Android version: >= 5.0 (API 21)
* Recommended Android version: >= 7.1 (API 25) - __*Launcher shortcuts*__

## Open Source Licenses

* __[ClearEditText](https://github.com/MrFuFuFu/ClearEditText)__ by *[Yuan Fu](https://github.com/MrFuFuFu)* - [APL 2.0](https://github.com/MrFuFuFu/ClearEditText)
* __[DNS66](https://github.com/julian-klode/dns66)__ by *[Julian Andres Klode](https://github.com/julian-klode)* - [GPLv3](https://github.com/julian-klode/dns66/blob/master/COPYING)
* __[Pcap4J](https://github.com/kaitoy/pcap4j)__ by *[Kaito Yamada](https://github.com/kaitoy)* - [MIT](https://github.com/kaitoy/pcap4j)
* __[MiniDNS](https://github.com/MiniDNS/minidns)__ by *[MiniDNS](https://github.com/MiniDNS)* - [APL 2.0](https://github.com/MiniDNS/minidns/blob/master/LICENCE_APACHE)
* __[Gson](https://github.com/google/gson)__ by *[Google](https://github.com/google)* - [APL 2.0](https://github.com/google/gson/blob/master/LICENSE)
* __[Shadowsocks](https://github.com/shadowsocks/shadowsocks-android)__ by *[Shadowsocks](https://github.com/shadowsocks)* - [GPLv3](https://github.com/shadowsocks/shadowsocks-android/blob/master/LICENSE)

## Credits

* __[JetBrains](https://www.jetbrains.com/)__ - For providing free license for [IntelliJ IDEA](https://www.jetbrains.com/idea/)

## License

    Copyright (C) 2017-2022 iTX Technologies <admin@itxtech.org>
    
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.

</details>
