//package com.mycz.krpc.core.registry.consul;
//
//import com.ecwid.consul.v1.ConsulClient;
//import com.ecwid.consul.v1.QueryParams;
//import com.ecwid.consul.v1.Response;
//import com.ecwid.consul.v1.health.HealthServicesRequest;
//import com.ecwid.consul.v1.health.model.HealthService;
//import com.mycz.krpc.core.registry.entity.ServiceDiscoveryResult;
//
//import java.util.List;
//import java.util.concurrent.*;
//
///**
// * Service服务监听
// */
//public class ServiceWatcher {
//
//    /**
//     * 可用服务
//     */
//    private static final ConcurrentHashMap<String, List<?>> availableService = new ConcurrentHashMap<>();
//
//    /**
//     * 狗狗登记册
//     */
//    private static final ConcurrentHashMap<String, Dog> dogs = new ConcurrentHashMap<>();
//
//    /**
//     * 狗狗执行小队
//     */
//    private static final ExecutorService dogExecutor = Executors.newFixedThreadPool(200);
//
//
//    public static void addWatch(String serviceName) {
//        Dog dog = dogs.get(serviceName);
//        if (dog != null) {
//            return;
//        }
//
//        synchronized (ServiceWatcher.class) {
//            dog = new Dog(serviceName);
//            dogExecutor.submit(new Dog(serviceName));
//            dogs.put(serviceName, dog);
//        }
//    }
//
//
//    /**
//     * 一个狗看一个服务
//     */
//    private static final class Dog implements Runnable {
//        // 服务名称
//        private final String serviceName;
//        private final ConsulClient client;
//
//        private volatile long lastIndex = 0;
//
//        Dog(String serviceName, ConsulClient client) {
//            this.serviceName = serviceName;
//            this.client = client;
//        }
//
//        @Override
//        public void run() {
//            // 随机抖动，避免所有 watcher 同时打到 Consul
//            try {
//                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 1500));
//            } catch (InterruptedException ignored) {
//                // ignored
//            }
//
//            while (true) {
//                try {
//                    HealthServicesRequest req = HealthServicesRequest.newBuilder()
//                            .setPassing(true)
//                            .setQueryParams(QueryParams.Builder.builder()
//                                    .setIndex(lastIndex)
//                                    .setWaitTime(10)
//                                    .build())
//                            .build();
//
//                    Response<List<HealthService>> resp = client.getHealthServices(serviceName, req);
//                    Long newIdx = resp.getConsulIndex();
//                    if (newIdx != null && !newIdx.equals(lastIndex)) {
//                        lastIndex = newIdx;
//                        List<ServiceDiscoveryResult> results = mapToResults(resp.getValue(), serviceName);
//                        putCache(serviceName, results);
//                        // 成功则重置回退
//                        backoffSec = 1;
//                    }
//                    // 如果没有变化，Consul 会在 waitTime 超时后返回；循环继续即可
//                } catch (Throwable ex) {
//                    try {
//                        Thread.sleep( 1000L);
//                    } catch (InterruptedException ignored) {
//
//                    }
//                }
//            }
//        }
//    }
//
//}
