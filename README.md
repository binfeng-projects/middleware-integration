
-------------------------------------------------------------------------------
## 关于本工程
### 作为[bf-framework](https://github.com/binfeng-projects/bf-framework)的对接桥梁工程,应该仍然由中间件团队维护
1. 该工程下的每个子模块都是平行等价的,都是作为bf-framework和真正业务开发团队的工程代码之间的一个桥梁。也就是说业务团队不应该直接依赖bf-framework。
一般情况下全公司只需要公用同一套中间件体系即可，但有的公司可能业务比较大，不同团队，对中间件的诉求不会完全一致，就可以分模块细分下打出特定包来供特定团队使用。
2. 该工程下的主体代码(这里忽略单元测试代码)其实是配置文件: yaml和pom。pom引入团队通用依赖。yaml配置公用的中间件。配置好后，业务团队就可以仅用一句话的配置启用
   任何需要的中间件和组件。如下：(配合Apollo，nacos等配置中心的中间件，还可以实现非常灵活的优先级覆盖和动态配置)
      ```yaml
      bf: #桥梁工程中配置了全公司都在用的数据源。本应用只需要，启用其中两个
         datasource:
           enabled: 'order-center,pay-center'
      bf: #桥梁工程中配置了全公司都在用的redis。本应用只需要，启用其中一个
         redis:
           enabled: 'trade-redis'
      ```
### 作为[bf-framework](https://github.com/binfeng-projects/bf-framework)的example示例对接工程
1. 这里就是主要看单元测试代码了。写了一些怎么使用的demo代码。都是运行通过的。没通过的会注明原因的需要条件（用到的中间件还是得自己搭起来啊，这个不用说把？i
测试环境或者自己玩，可以把docker这个利器用起来，秒秒钟搭起环境。
2. 单元测试代码可能比较乱，有些单元测试代码特别大 （比如TestBatch），没分模块。应该都是看得懂的。实际应用到业务的代码的时候，也可以记得来这里抄
（本人就是来这里抄代码）。框架和spring一起把不同中间件 的配置和使用都封装的比较统一了（就是拿到核心Bean,然后注入你的业务代码）。
3. 上面也说了，工程下的所有模块都是平行等价的。这里也只是用作示例。基本bf-middleware这个模块相当于整合的（其他两个模块要么做备份，要么做实验，稳定通过了
就往bf-middleware移）
4. 正常生产级别的业务就不要写这么多乱乱的单测代码了（虽然也不影响实际使用，因为打包不会打出单元测试代码），再次说明这里仅是因为时间问题，也不想开太多git项目
就把这里当做示例工程来用了。本工程主要还是传递一种桥梁和适配的思想(欢迎探讨)。

### 运行环境
1. 基本就是和[bf-framework](https://github.com/binfeng-projects/bf-framework)保持一致了（比如jdk17，参考该工程运行环境说明）。其他类库依赖
基本就是依赖bf-framework中的starters。
2. 当然除了starters也有些额外扩展，例如hadoop如果不是用原生hdfs存储，而是用云服务对象存储，那么还是需要引入一些三方类库的，bf-framework底层是不清楚你用了哪家存储 
的。这也是这个桥梁工程的一个非常大的意义，业务不应该关心这些依赖,另外玩过hadoop的人肯定都被各种jar包冲突折磨过（有的请在公屏扣1）。桥梁工程就可以把这些依赖冲突都弄干净。
3. 保持readme的干净。后续迭代更新方面的细节就放到change-log.txt中说明,主页说明些脉络性的整体feature
4. 如果有问题需要联系，可以先加QQ(wechat就不交换啦)， 搜索QID：luckybf008 验证信息说明:来自git。

------------------------------------------------------------------------------
## ⭐Star & donate
欢迎主页逛逛，看看项目。说不定你也遇到了我曾经遇到的问题，正需要。
爆肝不易。开源不易。如果本人的任何项目，任何文档，任何话术恰巧有帮助到你一点点的话，也可以考虑赞一杯星爸爸。
- [![zsm.jpg](https://cdn.jsdelivr.net/gh/luckybf/resource@main/pic/zsm.jpg)](https://smms.app/image/uy2lF5CLjpUsK83)
- [![zfb.png](https://cdn.jsdelivr.net/gh/luckybf/resource@main/pic/zfb.png)](https://smms.app/image/mE1LTl8UAeIGWJX)

------------------------------------------------------------------------------
## 如果上面打赏图片无法显示，可打开下面二维码
### <a target="_blank" href="https://g-dmwl1346.coding.net/public/java/bf-pom/git/files/main/zsm.jpg">wechatpay</a>
### <a target="_blank" href="https://g-dmwl1346.coding.net/public/java/bf-pom/git/files/main/zfb.png">alipay</a>
