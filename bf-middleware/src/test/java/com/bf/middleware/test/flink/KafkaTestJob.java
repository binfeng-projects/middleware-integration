package com.bf.middleware.test.flink;

import com.bf.BootStarter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.util.Collector;
import org.bf.framework.autoconfigure.flink.BaseFlinkJob;
import org.bf.framework.boot.util.SpringUtil;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class KafkaTestJob extends BaseFlinkJob {
    public KafkaTestJob(String jobName) {
        super(jobName);
    }

    public String classPathConfigFileKey() {
        return "flink/job1/flink-kafka";
    }
    @Override
    public void processJob() {
//        KafkaSourceBuilder<String> sourceBuilder = MiddlewareHolder.getKafkaSourceBuilder(FLINK_KAFKA_HZ);
        KafkaSourceBuilder<String> sourceBuilder = SpringUtil.getBean("bf.flink.kafka.hz_KafkaSourceBuilder", KafkaSourceBuilder.class);
        sourceBuilder.setTopics("dev-skywalking-logs-json");
//        KafkaSinkBuilder<String> sinkBuilder = MiddlewareHolder.getKafkaSinkBuilder(FLINK_KAFKA_HZ);;
//        sinkBuilder.setRecordSerializer(KafkaRecordSerializationSchema.builder()
//                .setTopic("dev-skywalking-logs-json")
//                .setValueSerializationSchema(new SimpleStringSchema())
//                .build());
        DataStreamSource<String> dataStream = env.fromSource(sourceBuilder.build(), WatermarkStrategy.noWatermarks(), "kafka-source1");
        DataStream<String> inputStream = dataStream.uid("test-job1").flatMap(new RichFlatMapFunction<String, String>() {
            @Override
            public void flatMap(String value, Collector<String> out) throws Exception {
                log.info(value);
                out.collect(value);
            }
        });
        inputStream.print();
//        final FileSink<String> sink = FileSink
//                .forRowFormat(new Path("/tmp/flink"), new SimpleStringEncoder<String>("UTF-8"))
//                .withRollingPolicy(
//                        DefaultRollingPolicy.builder()
//                                .withRolloverInterval(Duration.ofMinutes(15))
//                                .withInactivityInterval(Duration.ofMinutes(5))
//                                .withMaxPartSize(MemorySize.ofMebiBytes(1024))
//                                .build())
//                .build();
//
//        inputStream.sinkTo(sink);
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = BootStarter.run(null,args);
        KafkaTestJob job = new KafkaTestJob("kafka-test-job");
        job.run(args);
    }
}
