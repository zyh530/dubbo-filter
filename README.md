# dubbo 性能监控

## SPI

SPI 将https://github.com/zyh530/dubbo-filter 添加到已有项目即可，打包jar或者作为子项目均可。

参考了https://www.bilibili.com/video/BV1R34y1k7yN/的项目代码，对rpc进行方式进行服务实现，可以根据需要对项目代码进行修改，同样可以进行可视化展示

## dubbo-admin

Dubbo Admin 是 Dubbo 的可视化管理平台，可以用于管理 Dubbo 服务的注册中心、提供者和消费者等信息，以及查看 Dubbo 服务的运行状态和监控数据。Dubbo Admin 提供了 Web 界面和 RESTful API 接口，可以方便地进行 Dubbo 服务的管理和监控。Dubbo Admin 的源代码可以在 GitHub 上找到。

其中有服务统计模块(metrics) ,官方提供的dubbo-admin-server 输入ip报错，无法使用，没有办法使用，具体原因不明，因此打算使用metrics进行监控

## metrics

官方提供的metrics教程，需要Kubernetes 、Prometheus 进行部署展示，其中设计需要对上述技术有了解并且能够使用。官方文档只有简单描述，按照官方文档并不能实现。并且网络的教程不多，最关键的是，dubbo的github仓库变化较大，最新的博客 几乎没有

## monitor

官方提供的monitor只有2.5版本的，在启动注册后，同样不能使用，提示 org.apache.dubbo.common.URL，但实际是存在的该类的，应该是引用的jar包中缺少了依赖。

