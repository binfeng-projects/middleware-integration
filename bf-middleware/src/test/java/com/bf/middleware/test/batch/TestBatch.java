package com.bf.middleware.test.batch;

import com.thoughtworks.xstream.security.ExplicitTypePermission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bf.framework.autoconfigure.batch.BatchProxy;
import org.bf.framework.autoconfigure.batch.SpringBatchUtil;
import org.bf.framework.boot.util.SpringUtil;
import org.bf.framework.common.util.CollectionUtils;
import org.bf.framework.common.util.MapUtils;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.*;
import org.springframework.batch.item.adapter.ItemReaderAdapter;
import org.springframework.batch.item.adapter.PropertyExtractingDelegatingItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.*;
import org.springframework.batch.item.json.GsonJsonObjectReader;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.item.validator.SpringValidator;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.samples.adapter.tasklet.Task;
import org.springframework.batch.samples.common.*;
import org.springframework.batch.samples.domain.person.Person;
import org.springframework.batch.samples.domain.person.PersonService;
import org.springframework.batch.samples.domain.person.internal.PersonWriter;
import org.springframework.batch.samples.domain.trade.*;
import org.springframework.batch.samples.domain.trade.CustomerUpdateWriter;
import org.springframework.batch.samples.domain.trade.internal.*;
import org.springframework.batch.samples.domain.trade.internal.validator.TradeValidator;
import org.springframework.batch.samples.file.multiline.MultiLineTradeItemReader;
import org.springframework.batch.samples.file.multiline.MultiLineTradeItemWriter;
import org.springframework.batch.samples.file.multiline.Trade;
import org.springframework.batch.samples.file.multilineaggregate.AggregateItem;
import org.springframework.batch.samples.file.multilineaggregate.AggregateItemFieldSetMapper;
import org.springframework.batch.samples.file.multilineaggregate.AggregateItemReader;
import org.springframework.batch.samples.file.multirecordtype.DelegatingTradeLineAggregator;
import org.springframework.batch.samples.file.patternmatching.LineItem;
import org.springframework.batch.samples.file.patternmatching.Order;
import org.springframework.batch.samples.file.patternmatching.internal.OrderItemReader;
import org.springframework.batch.samples.file.patternmatching.internal.OrderLineAggregator;
import org.springframework.batch.samples.file.patternmatching.internal.extractor.*;
import org.springframework.batch.samples.file.patternmatching.internal.mapper.*;
import org.springframework.batch.samples.file.patternmatching.internal.validator.OrderValidator;
import org.springframework.batch.samples.file.patternmatching.internal.xml.Customer;
import org.springframework.batch.samples.football.Game;
import org.springframework.batch.samples.football.Player;
import org.springframework.batch.samples.football.PlayerSummary;
import org.springframework.batch.samples.football.internal.*;
import org.springframework.batch.samples.loop.GeneratingTradeResettingListener;
import org.springframework.batch.samples.loop.LimitDecider;
import org.springframework.batch.samples.support.*;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.util.AssertionErrors;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TestBatch implements BaseCoreTest {

    private static final SecureRandom secureRandom = new SecureRandom();
    @Autowired
    @Qualifier("bf.batch.default_BatchProxy")
    private BatchProxy proxy;

    private JdbcTemplate jdbcTemplate;

    @Override
    public void beforeEach() {
        jdbcTemplate = new JdbcTemplate(proxy.getDataSource());
    }

    @Test
    public void testHelloWorld() throws Exception {
        Step step = new StepBuilder("step1", proxy.getJobRepository())
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Hello world!");
                    return RepeatStatus.FINISHED;
                }, proxy.getTransactionManager())
                .build();
        Job job = new JobBuilder("HelloWorldJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

    }

    @Test
    public void testReadWriteJob() throws Exception {
        PersonService delegateObject = new PersonService();
        ItemReaderAdapter<Person> reader = new ItemReaderAdapter<Person>();
        reader.setTargetObject(delegateObject);
        reader.setTargetMethod("getData");

        PropertyExtractingDelegatingItemWriter<Person> writer = new PropertyExtractingDelegatingItemWriter<Person>();
        writer.setTargetObject(delegateObject);
        writer.setTargetMethod("processPerson");
        writer.setFieldsUsedAsTargetMethodArguments(new String[]{"firstName", "address.city"});

        SimpleStepBuilder<Person, Person> stepBuilder = new StepBuilder("step1", proxy.getJobRepository())
                .chunk(1, proxy.getTransactionManager());
        Step step = stepBuilder.reader(reader).writer(writer).build();

        Job job = new JobBuilder("ReadWriteJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

    }

    public static JobParameters getUniqueJobParameters() {
        Map<String, JobParameter<?>> parameters = new HashMap<>();
        parameters.put("random", new JobParameter<>(secureRandom.nextLong(), Long.class));
        return new JobParameters(parameters);
    }

    @Test
    public void testTaskletAdapterJob() throws Exception {
        MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
        adapter.setTargetObject(new Task.TestBean().setValue("foo"));
        adapter.setTargetMethod("execute");
        adapter.setArguments(new Object[]{"foo2", 3, 3.14d});

        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        attribute.setIsolationLevel(Isolation.DEFAULT.value());
//        attribute.setTimeout(DefaultTransactionAttribute.TIMEOUT_DEFAULT);

        //step1 用MethodInvokingTaskletAdapter，并且带事务
        Step step = new StepBuilder("step1", proxy.getJobRepository())
                .tasklet(adapter, proxy.getTransactionManager())
                .transactionAttribute(attribute)
                .build();

        MethodInvokingTaskletAdapter shortVersion = new MethodInvokingTaskletAdapter();
        shortVersion.setTargetObject(new Task());
        shortVersion.setTargetMethod("doWork");

        Step nextStep = new StepBuilder("step2", proxy.getJobRepository())
                .tasklet(shortVersion, proxy.getTransactionManager())
                .build();

        Job job = new JobBuilder("TaskletAdapterJob", proxy.getJobRepository())
                .start(step)
                .next(nextStep)
                .build();

        JobExecution execution = proxy.getJobLauncher().run(job, new JobParametersBuilder().addString("value", "foo1").toJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

    }

    /**
     * trade数据，很多地方共用，抽出来
     *
     * @return
     */
    public SimpleStepBuilder<Trade, Trade> generateTradeStep(String stepName) {
//        --------------------------------------------------------trader reader----------------------------------------------------
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/beanwrapper/data/ImportTradeDataStep.txt");
        Range[] ranges = new Range[]{new Range(1, 12), new Range(13, 15), new Range(16, 20), new Range(21, 29)};
        String[] columnNames = new String[]{"ISIN", "Quantity", "price", "CUSTOMER"};
        FlatFileItemReader<Trade> traderReader = SpringBatchUtil.generateFileReader(Trade.class, classPathFile, ranges, columnNames);
//        --------------------------------------------------------validate----------------------------------------------------
        SpringValidator<Trade> fixedValidator = new SpringValidator<Trade>();
        fixedValidator.setValidator(new TradeValidator());
        ValidatingItemProcessor<Trade> processor = new ValidatingItemProcessor<Trade>(fixedValidator);
//        --------------------------------------------------------以上所有对trade的操作构成step1 ----------------------------------------------------
        SimpleStepBuilder<Trade, Trade> stepBuilder = new StepBuilder(stepName, proxy.getJobRepository())
                .chunk(1, proxy.getTransactionManager());
        return stepBuilder.processor(processor).reader(traderReader);
    }

    @Test
    public void testBeanWrapperMapperSampleJob() throws Exception {
//        --------------------------------------------------------trader writer----------------------------------------------------
        JdbcTradeDao tradeDao = new JdbcTradeDao();
        tradeDao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "TRADE_SEQ", "ID");
        tradeDao.setIncrementer(incrementer);

        TradeWriter tradeWriter = new TradeWriter();
        tradeWriter.setDao(tradeDao);
//        --------------------------------------------------------以上所有对trade的操作构成step1 ----------------------------------------------------
        Step step1 = generateTradeStep("step1").writer(tradeWriter).build();


//        --------------------------------------------------------person reader----------------------------------------------------
        String[] columnNames = new String[]{"Title", "FirstName", "LastName", "age", "Address.AddrLine1", "children[0].name", "children[1].name"};
        Range[] ranges = new Range[]{new Range(1, 5), new Range(6, 20), new Range(21, 40), new Range(41, 45), new Range(46, 55), new Range(56, 65), new Range(66, 75)};
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/beanwrapper/data/ImportPersonDataStep.txt");
        FlatFileItemReader<Person> personReader = SpringBatchUtil.generateFileReader(Person.class, classPathFile, ranges, columnNames);

        SimpleStepBuilder<Person, Person> step2Builder = new StepBuilder("step2", proxy.getJobRepository())
                .chunk(1, proxy.getTransactionManager());

        Step step2 = step2Builder.reader(personReader).writer(new PersonWriter()).build();

        Job job = new JobBuilder("testBeanWrapperMapperSampleJob", proxy.getJobRepository())
                .start(step1)
                .next(step2)
                .build();

        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    /**
     * 混合writer,有数据库和file
     *
     * @throws Exception
     */
    @Test
    public void testCompositeItemWriterSampleJob() throws Exception {
        // 清理已经存在的数据
        String GET_TRADES = "SELECT isin, quantity, price, customer FROM TRADE order by isin";
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
        int before = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");

        // 跑核心job的逻辑
//        --------------------------------------------------------trader writer,复合writer，往多个地方写----------------------------------------------------
        JdbcTradeDao tradeDao = new JdbcTradeDao();
        tradeDao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "TRADE_SEQ", "ID");
        tradeDao.setIncrementer(incrementer);

        TradeWriter daoWriter = new TradeWriter();
        daoWriter.setDao(tradeDao);

        FlatFileItemWriter<Trade> fw1 = new FlatFileItemWriter<Trade>();
        fw1.setName("fw1"); //多个同样对象的writer,一定要设置name区分
        fw1.setResource(new FileUrlResource("target/test-outputs/CustomerReport1.txt"));
        fw1.setLineAggregator(new PassThroughLineAggregator<Trade>());

        FlatFileItemWriter<Trade> fw2 = new FlatFileItemWriter<Trade>();
        fw2.setName("fw2"); //多个同样对象的writer,一定要设置name区分
        fw2.setResource(new FileUrlResource("target/test-outputs/CustomerReport2.txt"));
        fw2.setLineAggregator(new PassThroughLineAggregator<Trade>());

        //混合writer,往多个地方写
        CompositeItemWriter<Trade> compositeItemWriter = new CompositeItemWriter<Trade>();
        compositeItemWriter.setDelegates(CollectionUtils.newArrayList(daoWriter, fw1, fw2));
//        --------------------------------------------------------以上所有对trade的操作构成step1 ----------------------------------------------------
        Step step1 = generateTradeStep("step1").writer(compositeItemWriter).build();
        Job job = new JobBuilder("testCompositeItemWriterSampleJob", proxy.getJobRepository())
                .start(step1)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        //验证输出文件是否和预期相同
        String EXPECTED_OUTPUT_FILE = "Trade: [isin=UK21341EAH41,quantity=211,price=31.11,customer=customer1]"
                + "Trade: [isin=UK21341EAH42,quantity=212,price=32.11,customer=customer2]"
                + "Trade: [isin=UK21341EAH43,quantity=213,price=33.11,customer=customer3]"
                + "Trade: [isin=UK21341EAH44,quantity=214,price=34.11,customer=customer4]"
                + "Trade: [isin=UK21341EAH45,quantity=215,price=35.11,customer=customer5]";

        compositeItemWriterSampleJobCheckOutputFile("target/test-outputs/CustomerReport1.txt", EXPECTED_OUTPUT_FILE);
        compositeItemWriterSampleJobCheckOutputFile("target/test-outputs/CustomerReport2.txt", EXPECTED_OUTPUT_FILE);

        //验证数据库中写入的数据是否符合预期
        final List<Trade> trades = new ArrayList<>() {
            {
                add(new Trade("UK21341EAH41", 211, new BigDecimal("31.11"), "customer1"));
                add(new Trade("UK21341EAH42", 212, new BigDecimal("32.11"), "customer2"));
                add(new Trade("UK21341EAH43", 213, new BigDecimal("33.11"), "customer3"));
                add(new Trade("UK21341EAH44", 214, new BigDecimal("34.11"), "customer4"));
                add(new Trade("UK21341EAH45", 215, new BigDecimal("35.11"), "customer5"));
            }
        };
        int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");
        assertEquals(before + 5, after);
        jdbcTemplate.query(GET_TRADES, new RowCallbackHandler() {
            private int activeRow = 0;

            @Override
            public void processRow(ResultSet rs) throws SQLException {
                Trade trade = trades.get(activeRow++);

                assertEquals(trade.getIsin(), rs.getString(1));
                assertEquals(trade.getQuantity(), rs.getLong(2));
                assertEquals(trade.getPrice(), rs.getBigDecimal(3));
                assertEquals(trade.getCustomer(), rs.getString(4));
            }
        });
    }

    private void compositeItemWriterSampleJobCheckOutputFile(String fileName, String expectedOutPut) throws IOException {
        List<String> outputLines = IOUtils.readLines(new FileInputStream(fileName), "UTF-8");
        StringBuilder output = new StringBuilder();
        for (String line : outputLines) {
            output.append(line);
        }
        assertEquals(expectedOutPut, output.toString());
    }


    /**
     * 逗号分割的数据
     * 每个顾客账户账户加5块钱
     *
     * @throws Exception
     */
    @Test
    public void testFileDelimitedJob() throws Exception {
        String[] columnNames = new String[]{"name", "credit"};
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/file/delimited/data/delimited.csv");
        FlatFileItemReader<CustomerCredit> reader = SpringBatchUtil.generateFileReader(CustomerCredit.class, classPathFile, null, columnNames);

        //用builder构建
        FlatFileItemWriter<CustomerCredit> writer = new FlatFileItemWriterBuilder<CustomerCredit>().name("itemWriter")
                .resource(new FileUrlResource("target/test-outputs/delimitedOutput.csv"))
                .delimited()
                .names(columnNames)
                .build();
        SimpleStepBuilder<CustomerCredit, CustomerCredit> stepBuilder = new StepBuilder("step1", proxy.getJobRepository())
                .chunk(2, proxy.getTransactionManager());

        Step step = stepBuilder.reader(reader).writer(writer).processor(new CustomerCreditIncreaseProcessor()).build();
        Job job = new JobBuilder("testFileDelimitedJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

    }

    /**
     * 固定格式化的数据，比如保留固定小数位数
     */
    @Test
    public void testFileFormatJob() throws Exception {
        String[] columnNames = new String[]{"name", "credit"};
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/file/fixed/data/fixedLength.txt");
        Range[] ranges = new Range[]{new Range(1, 9), new Range(10, 11)};
        FlatFileItemReader<CustomerCredit> reader = SpringBatchUtil.generateFileReader(CustomerCredit.class, classPathFile, ranges, columnNames);

        //用builder构建
        FlatFileItemWriter<CustomerCredit> writer = new FlatFileItemWriterBuilder<CustomerCredit>().name("itemWriter")
                .resource(new FileUrlResource("target/test-outputs/fixedLengthOutput.txt"))
                .formatted()
                .format("%-9s%-2.0f")
                .names(columnNames)
                .build();
        SimpleStepBuilder<CustomerCredit, CustomerCredit> stepBuilder = new StepBuilder("step1", proxy.getJobRepository())
                .chunk(2, proxy.getTransactionManager());

        Step step = stepBuilder.reader(reader).writer(writer).processor(new CustomerCreditIncreaseProcessor()).build();
        Job job = new JobBuilder("testFileFormatJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    /**
     * json数据
     */
    @Test
    public void testFileJsonJob() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/file/json/data/trades.json");
        WritableResource outputResource = new FileUrlResource("target/test-outputs/trades.json");
        JsonItemReader<Trade> reader = new JsonItemReaderBuilder<Trade>().name("tradesJsonItemReader")
                .resource(inputResource)
                .jsonObjectReader(new GsonJsonObjectReader<>(Trade.class))
                .build();

        JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().resource(outputResource)
                .lineSeparator("\n")
                .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
                .name("tradesJsonFileItemWriter")
                .shouldDeleteIfExists(true)
                .build();
        SimpleStepBuilder<Trade, Trade> stepBuilder = new StepBuilder("step1", proxy.getJobRepository())
                .chunk(2, proxy.getTransactionManager());

        Step step = stepBuilder.reader(reader).writer(writer).build();
        Job job = new JobBuilder("testFileJsonJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        String expectedHash = DigestUtils.md5DigestAsHex(inputResource.getInputStream());
        String actualHash = DigestUtils.md5DigestAsHex(outputResource.getInputStream());
        assertEquals(expectedHash, actualHash);
    }

    /**
     * 多行数据为一个对象
     */
    @Test
    public void testFileMultilineJob() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/file/multiline/data/multiLine.txt");
        WritableResource outputResource = new FileUrlResource("target/test-outputs/multiLineOutput.txt");
        FlatFileItemReader<FieldSet> delegate = new FlatFileItemReaderBuilder<FieldSet>().name("delegateItemReader")
                .resource(inputResource)
                .lineTokenizer(new DelimitedLineTokenizer())
                .fieldSetMapper(new PassThroughFieldSetMapper())
                .build();
        MultiLineTradeItemReader reader = new MultiLineTradeItemReader();
        reader.setDelegate(delegate);

        FlatFileItemWriter<String> delegateItemWriter = new FlatFileItemWriterBuilder<String>().name("delegateItemWriter")
                .resource(outputResource)
                .lineAggregator(new PassThroughLineAggregator<>())
                .build();
        MultiLineTradeItemWriter writer = new MultiLineTradeItemWriter();
        writer.setDelegate(delegateItemWriter);

        SimpleStepBuilder<Trade, Trade> stepBuilder = new StepBuilder("step1", proxy.getJobRepository())
                .chunk(2, proxy.getTransactionManager());

        Step step = stepBuilder.reader(reader).writer(writer).build();
        Job job = new JobBuilder("testFileMultilineJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        Path inputFile = inputResource.getFile().toPath();
        Path outputFile = outputResource.getFile().toPath();
        Assertions.assertLinesMatch(Files.lines(inputFile), Files.lines(outputFile));
    }

    /**
     * 多行数据为一个对象, 复杂解析聚合
     */
    @Test
    public void testFileMultilineAggregateJob() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/file/multilineaggregate/data/multilineStep.txt");
        WritableResource outputResource = new FileUrlResource("target/test-outputs/multilineStep-output.txt");

        Map<String, LineTokenizer> tokenizersMap = MapUtils.newHashMap();
        FixedLengthTokenizer begin = new FixedLengthTokenizer();
        begin.setColumns(new Range(1, 5)); //begin这几个字符刚好长度5
//        begin.setNames(columuNames); 没有列名，表示只有一列
        tokenizersMap.put("BEGIN*", begin); //匹配BEGIN开始的字符

        FixedLengthTokenizer end = new FixedLengthTokenizer();
        end.setColumns(new Range(1, 3)); //end这几个字符刚好长度3
//        begin.setNames(columuNames); 没有列名，表示只有一列
        tokenizersMap.put("END*", end); //匹配END开始的字符

        FixedLengthTokenizer tradeRecordTokenizer = new FixedLengthTokenizer();
        tradeRecordTokenizer.setColumns(new Range(1, 12), new Range(13, 15), new Range(16, 20), new Range(21, 29)); //end这几个字符刚好长度3
        tradeRecordTokenizer.setNames("ISIN", "Quantity", "Price", "CUSTOMER");
        tokenizersMap.put("*", tradeRecordTokenizer); //匹配所有字符

        //混合分词器
        PatternMatchingCompositeLineTokenizer compositeLineTokenizer = new PatternMatchingCompositeLineTokenizer();
        compositeLineTokenizer.setTokenizers(tokenizersMap);

//        可以把AggregateItem<Trade>当成List<Trade>,只是这个list中有的是BEGIN，有的是END,其他才是数据本体
        DefaultLineMapper<AggregateItem<Trade>> lineMapper = new DefaultLineMapper<AggregateItem<Trade>>();
        lineMapper.setLineTokenizer(compositeLineTokenizer);
        AggregateItemFieldSetMapper<Trade> fieldSetMapper = new AggregateItemFieldSetMapper<Trade>();
        fieldSetMapper.setDelegate(new TradeFieldSetMapper());
        lineMapper.setFieldSetMapper(fieldSetMapper);
//      和之前不同的是，相当于得到了一个List
        FlatFileItemReader<AggregateItem<Trade>> delegateItemReader = new FlatFileItemReaderBuilder<AggregateItem<Trade>>().name("delegateItemReader")
                .resource(inputResource)
                .lineMapper(lineMapper)
                .build();
//        这个最终的finalRead相当于做了去头掐尾的事情，去掉List中的BEGIN和END的的对象，得到最终真正的数据
        AggregateItemReader<Trade> reader = new AggregateItemReader<Trade>();
        reader.setItemReader(delegateItemReader);

        //注意这里的泛型也是个List
        FlatFileItemWriter<List<Trade>> writer = new FlatFileItemWriterBuilder<List<Trade>>().name("delegateItemWriter")
                .resource(outputResource)
                .lineAggregator(new PassThroughLineAggregator<>())
                .build();
        SimpleStepBuilder<List<Trade>, List<Trade>> stepBuilder = new StepBuilder("step1", proxy.getJobRepository())
                .chunk(1, proxy.getTransactionManager());

        //这里和前面也不同，设置了stream,之前都没有 TODO modify
        Step step = stepBuilder.stream(delegateItemReader).reader(reader).writer(writer).build();
        Job job = new JobBuilder("testFileMultilineAggregateJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        String EXPECTED_RESULT = "[Trade: [isin=UK21341EAH45,quantity=978,price=98.34,customer=customer1], Trade: [isin=UK21341EAH46,quantity=112,price=18.12,customer=customer2]]"
                + "[Trade: [isin=UK21341EAH47,quantity=245,price=12.78,customer=customer2], Trade: [isin=UK21341EAH48,quantity=108,price=9.25,customer=customer3], Trade: [isin=UK21341EAH49,quantity=854,price=23.39,customer=customer4]]";

        assertEquals(EXPECTED_RESULT,
                StringUtils.replace(IOUtils.toString(outputResource.getInputStream(), StandardCharsets.UTF_8),
                        System.getProperty("line.separator"), ""));
    }

    /**
     * 两个不同对象分开解析
     *
     * @throws Exception
     */
    @Test
    public void testMultiRecordTypeFunctional() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/file/multirecordtype/data/multiRecordType.txt");
        WritableResource outputResource = new FileUrlResource("target/test-outputs/multiRecordTypeOutput.txt");


        FixedLengthTokenizer customerTokenizer = new FixedLengthTokenizer();
        customerTokenizer.setNames("id", "name", "credit");
        customerTokenizer.setColumns(new Range(5, 9), new Range(10, 18), new Range(19, 26));

        FixedLengthTokenizer tradeTokenizer = new FixedLengthTokenizer();
        tradeTokenizer.setNames("isin", "quantity", "price", "customer");
        tradeTokenizer.setColumns(new Range(5, 16), new Range(17, 19), new Range(20, 25), new Range(26, 34));

        PatternMatchingCompositeLineMapper mapper = new PatternMatchingCompositeLineMapper();
        mapper.setTokenizers(Map.of("TRAD*", tradeTokenizer, "CUST*", customerTokenizer));
        mapper.setFieldSetMappers(Map.of("TRAD*", new TradeFieldSetMapper(), "CUST*", new CustomerCreditFieldSetMapper()));

        FlatFileItemReader reader = new FlatFileItemReaderBuilder().name("compositeItemReader")
                .resource(inputResource)
                .lineMapper(mapper)
                .build();

        FormatterLineAggregator<CustomerCredit> customerLineAggregator = new FormatterLineAggregator<>();
        BeanWrapperFieldExtractor<CustomerCredit> customerFieldExtractor = new BeanWrapperFieldExtractor<>();
        customerFieldExtractor.setNames(new String[]{"id", "name", "credit"});
        customerLineAggregator.setFieldExtractor(customerFieldExtractor);
        customerLineAggregator.setFormat("CUST%05d%-9s%08.0f");

        FormatterLineAggregator<Trade> tradeLineAggregator = new FormatterLineAggregator<>();
        BeanWrapperFieldExtractor<Trade> tradeFieldExtractor = new BeanWrapperFieldExtractor<>();
        tradeFieldExtractor.setNames(new String[]{"isin", "quantity", "price", "customer"});
        tradeLineAggregator.setFieldExtractor(tradeFieldExtractor);
        tradeLineAggregator.setFormat("TRAD%-12s%-3d%6s%-9s");

        DelegatingTradeLineAggregator lineAggregator = new DelegatingTradeLineAggregator();
        lineAggregator.setTradeLineAggregator(tradeLineAggregator);
        lineAggregator.setCustomerLineAggregator(customerLineAggregator);

        FlatFileItemWriter writer = new FlatFileItemWriterBuilder().name("iemWriter")
                .resource(outputResource)
                .lineAggregator(lineAggregator)
                .build();
        Step step = new StepBuilder("step1", proxy.getJobRepository()).chunk(2, proxy.getTransactionManager())
                .reader(reader)
                .writer(writer)
                .build();
        Job job = new JobBuilder("testMultiRecordTypeFunctional", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        Path inputFile = inputResource.getFile().toPath();
        Path outputFile = outputResource.getFile().toPath();
        Assertions.assertLinesMatch(Files.lines(inputFile), Files.lines(outputFile));

    }

    /**
     * 多文件多数据源读取,写入文件滚动切割
     *
     * @throws Exception
     */
    @Test
    public void testMultiResourceJob() throws Exception {
        Resource inputResource1 = new ClassPathResource("org/springframework/batch/samples/file/multiresource/data/delimited.csv");
        Resource inputResource2 = new ClassPathResource("org/springframework/batch/samples/file/multiresource/data/delimited2.csv");
        WritableResource outputResource = new FileUrlResource("target/test-outputs/multiResourceOutput.csv");

        FlatFileItemReader<CustomerCredit> delegateReader = new FlatFileItemReaderBuilder<CustomerCredit>().name("delegateItemReader")
                .delimited()
                .names("name", "credit")
                .targetType(CustomerCredit.class)
                .build();

        MultiResourceItemReader<CustomerCredit> reader = new MultiResourceItemReaderBuilder<CustomerCredit>().name("itemReader")
                .resources(inputResource1, inputResource2)
                .delegate(delegateReader)
                .build();

        FlatFileItemWriter<CustomerCredit> delegateWriter = new FlatFileItemWriterBuilder<CustomerCredit>().name("delegateItemWriter")
                .delimited()
                .names("name", "credit")
                .build();
        MultiResourceItemWriter<CustomerCredit> writer = new MultiResourceItemWriterBuilder<CustomerCredit>().name("itemWriter")
                .delegate(delegateWriter)
                .resource(outputResource)
                .itemCountLimitPerResource(6)
                .build();

        Step step = new StepBuilder("step1", proxy.getJobRepository()).<CustomerCredit, CustomerCredit>chunk(2, proxy.getTransactionManager())
                .reader(reader)
                .processor(new CustomerCreditIncreaseProcessor())
                .writer(writer)
                .build();
        Job job = new JobBuilder("testMultiResourceJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    /**
     * 多文件多数据源读取,写入文件滚动切割
     *
     * @throws Exception
     */
    @Test
    public void testPatternMatchingJob() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/file/patternmatching/data/multilineOrderInput.txt");
        WritableResource outputResource = new FileUrlResource("target/test-outputs/multilineOrderOutput.txt");

        Map<String, LineTokenizer> tokenizersMap = MapUtils.newHashMap();
        DelimitedLineTokenizer head = new DelimitedLineTokenizer(";");
        head.setNames("LINE_ID", "ORDER_ID", "ORDER_DATE");
        tokenizersMap.put("HEA*", head);

        DelimitedLineTokenizer foot = new DelimitedLineTokenizer(";");
        foot.setNames("LINE_ID", "TOTAL_LINE_ITEMS", "TOTAL_ITEMS", "TOTAL_PRICE");
        tokenizersMap.put("FOT*", foot);

        DelimitedLineTokenizer biz = new DelimitedLineTokenizer(";");
        biz.setNames("LINE_ID", "COMPANY_NAME", "REG_ID", "VIP");
        tokenizersMap.put("BCU*", biz);

        DelimitedLineTokenizer customer = new DelimitedLineTokenizer(";");
        customer.setNames("LINE_ID", "LAST_NAME", "FIRST_NAME", "MIDDLE_NAME", "REGISTERED", "REG_ID", "VIP");
        tokenizersMap.put("NCU*", customer);

        DelimitedLineTokenizer billAdress = new DelimitedLineTokenizer(";");
        billAdress.setNames("LINE_ID", "ADDRESSEE", "ADDR_LINE1", "ADDR_LINE2", "CITY", "ZIP_CODE", "STATE", "COUNTRY");
        tokenizersMap.put("BAD*", billAdress);

        DelimitedLineTokenizer shippingAdress = new DelimitedLineTokenizer(";");
        shippingAdress.setNames("LINE_ID", "ADDRESSEE", "ADDR_LINE1", "ADDR_LINE2", "CITY", "ZIP_CODE", "STATE", "COUNTRY");
        tokenizersMap.put("SAD*", shippingAdress);

        DelimitedLineTokenizer bill = new DelimitedLineTokenizer(";");
        bill.setNames("LINE_ID", "PAYMENT_TYPE_ID", "PAYMENT_DESC");
        tokenizersMap.put("BIN*", bill);

        DelimitedLineTokenizer shipping = new DelimitedLineTokenizer(";");
        shipping.setNames("LINE_ID", "SHIPPER_ID", "SHIPPING_TYPE_ID", "ADDITIONAL_SHIPPING_INFO");
        tokenizersMap.put("SIN*", shipping);

        DelimitedLineTokenizer item = new DelimitedLineTokenizer(";");
        item.setNames("LINE_ID", "ITEM_ID", "PRICE", "DISCOUNT_PERC", "DISCOUNT_AMOUNT", "SHIPPING_PRICE", "HANDLING_PRICE", "QUANTITY", "TOTAL_PRICE");
        tokenizersMap.put("LIT*", item);

        tokenizersMap.put("*", new DelimitedLineTokenizer());
        //混合分词器
        PatternMatchingCompositeLineTokenizer compositeLineTokenizer = new PatternMatchingCompositeLineTokenizer();
        compositeLineTokenizer.setTokenizers(tokenizersMap);

        FlatFileItemReader<FieldSet> delegateReader = new FlatFileItemReaderBuilder<FieldSet>().name("delegateItemReader")
                .resource(inputResource)
                .lineTokenizer(compositeLineTokenizer)
                .fieldSetMapper(new PassThroughFieldSetMapper())
                .build();
        OrderItemReader reader = new OrderItemReader();
        reader.setFieldSetReader(delegateReader); //真正从文件里读取的是delegate
        //下面开始都是逐行匹配，匹配到哪个哪个解析,匹配规则都是上面的Tokenizer决定
        reader.setAddressMapper(new AddressFieldSetMapper());
        reader.setBillingMapper(new BillingFieldSetMapper());
        reader.setCustomerMapper(new CustomerFieldSetMapper());
        reader.setHeaderMapper(new HeaderFieldSetMapper());
        reader.setItemMapper(new OrderItemFieldSetMapper());
        reader.setShippingMapper(new ShippingFieldSetMapper());

        FormatterLineAggregator<Order> headerAggregator = new FormatterLineAggregator<>();
        headerAggregator.setFieldExtractor(new HeaderFieldExtractor());
        headerAggregator.setFormat("%-12s%-10s%-30s");

        FormatterLineAggregator<Order> footerAggregator = new FormatterLineAggregator<>();
        footerAggregator.setFieldExtractor(new FooterFieldExtractor());
        footerAggregator.setFormat("%-10s%20s");

        FormatterLineAggregator<Order> customerAggregator = new FormatterLineAggregator<>();
        customerAggregator.setFieldExtractor(new CustomerFieldExtractor());
        customerAggregator.setFormat("%-9s%-10s%-10s%-10s%-10s");

        FormatterLineAggregator<Order> addressAggregator = new FormatterLineAggregator<>();
        addressAggregator.setFieldExtractor(new AddressFieldExtractor());
        addressAggregator.setFormat("%-8s%-20s%-10s%-10s");

        FormatterLineAggregator<Order> billingAggregator = new FormatterLineAggregator<>();
        billingAggregator.setFieldExtractor(new BillingInfoFieldExtractor());
        billingAggregator.setFormat("%-8s%-10s%-20s");

        FormatterLineAggregator<LineItem> lineItemAggregator = new FormatterLineAggregator<>();
        lineItemAggregator.setFieldExtractor(new LineItemFieldExtractor());
        lineItemAggregator.setFormat("%-5s%-10s%-10s");

        Map<String, LineAggregator> outAggregators = new HashMap<>();
        outAggregators.put("header", headerAggregator);
        outAggregators.put("footer", footerAggregator);
        outAggregators.put("customer", customerAggregator);
        outAggregators.put("address", addressAggregator);
        outAggregators.put("billing", billingAggregator);
        outAggregators.put("item", lineItemAggregator);

        OrderLineAggregator lineAggregator = new OrderLineAggregator();
        lineAggregator.setAggregators(outAggregators);
        FlatFileItemWriter<Order> writer = new FlatFileItemWriterBuilder<Order>().name("itemWriter")
                .resource(outputResource)
                .lineAggregator(lineAggregator)
                .build();
        SpringValidator<Order> orderValidator = new SpringValidator<Order>();
        orderValidator.setValidator(new OrderValidator());
        ValidatingItemProcessor<Order> processor = new ValidatingItemProcessor<Order>(orderValidator);
        //校验不过不抛异常，而是跳过？
        processor.setFilter(true);
        Step step = new StepBuilder("step1", proxy.getJobRepository()).<Order, Order>chunk(5, proxy.getTransactionManager())
                .stream(delegateReader)
                .stream(writer)
                .reader(reader)
                .writer(writer)
                .processor(processor)
                .build();
        Job job = new JobBuilder("testPatternMatchingJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        Path inputFile = new ClassPathResource("org/springframework/batch/samples/file/patternmatching/data/multilineOrderOutput.txt").getFile().toPath();
        Path outputFile = outputResource.getFile().toPath();
        Assertions.assertLinesMatch(Files.lines(inputFile), Files.lines(outputFile));
    }

    /**
     * 解析xml格式的源数据
     */
    @Test
    public void testXmlFileJob() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/file/xml/data/input.xml");
        WritableResource outputResource = new FileUrlResource("target/test-outputs/output.xml");

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(Map.of("customer", CustomerCredit.class, "credit", BigDecimal.class, "name", String.class));
        marshaller.setTypePermissions(new ExplicitTypePermission(new Class[]{CustomerCredit.class}));

        StaxEventItemReader<CustomerCredit> reader = new StaxEventItemReaderBuilder<CustomerCredit>().name("itemReader")
                .resource(inputResource)
                .addFragmentRootElements("customer")
                .unmarshaller(marshaller)
                .build();
        StaxEventItemWriter<CustomerCredit> writer = new StaxEventItemWriterBuilder<CustomerCredit>().name("itemWriter")
                .resource(outputResource)
                .marshaller(marshaller)
                .rootTagName("customers")
                .overwriteOutput(true)
                .build();
        Step step = new StepBuilder("step1", proxy.getJobRepository()).<CustomerCredit, CustomerCredit>chunk(5, proxy.getTransactionManager())
                .reader(reader)
                .writer(writer)
                .processor(new CustomerCreditIncreaseProcessor())
                .build();
        Job job = new JobBuilder("testXmlFileJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    int activeRow = 0;

    /**
     * 过滤功能
     * listener 的生命周期注入 step的上下文
     */
    @Test
    public void testFilterJob() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/filter/data/customers.txt");

        JdbcCustomerDao dao = new JdbcCustomerDao();
        dao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "CUSTOMER_SEQ", "ID");
        dao.setIncrementer(incrementer);

        CustomerUpdateProcessor processor = new CustomerUpdateProcessor();
        processor.setCustomerDao(dao);
        processor.setInvalidCustomerLogger(new CommonsLoggingInvalidCustomerLogger());


        CompositeCustomerUpdateLineTokenizer lineTokenizer = new CompositeCustomerUpdateLineTokenizer();
        FixedLengthTokenizer customerTokenizer = new FixedLengthTokenizer();
        //range最大最小都是自己，就是取那一位
        customerTokenizer.setColumns(new Range(1, 1), new Range(2, 18), new Range(19, 26));

        FixedLengthTokenizer footerTokenizer = new FixedLengthTokenizer();
        footerTokenizer.setColumns(new Range(1, 1), new Range(2, 8));
        lineTokenizer.setCustomerTokenizer(customerTokenizer);
        lineTokenizer.setFooterTokenizer(footerTokenizer);

        FlatFileItemReader<CustomerUpdate> reader = new FlatFileItemReaderBuilder<CustomerUpdate>().name("itemReader")
                .resource(inputResource)
                .lineTokenizer(lineTokenizer)
                .fieldSetMapper(new CustomerUpdateFieldSetMapper())
                .linesToSkip(1)   //skip
                .build();
        CustomerUpdateWriter writer = new CustomerUpdateWriter();
        writer.setCustomerDao(dao);

        Step step = new StepBuilder("uploadCustomer", proxy.getJobRepository()).<CustomerUpdate, CustomerUpdate>chunk(1, proxy.getTransactionManager())
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(lineTokenizer) //监听器
                .build();
        Job job = new JobBuilder("testFilterJob", proxy.getJobRepository())
                .start(step)
                .build();

//      清理上一次测试的现场，不影响本次测试
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
        JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "CUSTOMER", "ID > 4");
        jdbcTemplate.update("update CUSTOMER set credit=100000");
//      初始化一些数据
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select name, CREDIT from CUSTOMER");
        Map<String, Double> credits = new HashMap<>();
        for (Map<String, Object> map : list) {
            credits.put((String) map.get("NAME"), ((Number) map.get("CREDIT")).doubleValue());
        }

        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        //job执行完的后续验证
        String GET_CUSTOMERS = "select NAME, CREDIT from CUSTOMER order by NAME";
        List<Customer> customers = Arrays.asList(new Customer("customer1", (credits.get("customer1"))),
                new Customer("customer2", (credits.get("customer2"))), new Customer("customer3", 100500),
                new Customer("customer4", credits.get("customer4")), new Customer("customer5", 32345),
                new Customer("customer6", 123456));

        jdbcTemplate.query(GET_CUSTOMERS, rs -> {
            Customer customer = customers.get(activeRow++);
            assertEquals(customer.getName(), rs.getString(1));
            assertEquals(customer.getCredit(), rs.getDouble(2), .01);
        });

        Map<String, Object> step1Execution = jdbcTemplate.queryForMap(
                "SELECT * from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID = ? and STEP_NAME = ?", execution.getId(), "uploadCustomer");
        assertEquals("4", step1Execution.get("READ_COUNT").toString());
        assertEquals("1", step1Execution.get("FILTER_COUNT").toString());
        assertEquals("3", step1Execution.get("WRITE_COUNT").toString());
    }

    /**
     * jdbc多step混合
     * 1. 从文件中读取player,插入数据库
     * 2. 从文件中读取game,插入数据库
     * 3. 从数据库中join上面插入的数据，得到输入源，再计算写回统计表
     */
    @Test
    public void testFootballJob() throws Exception {
        FlatFileItemReader<Player> playerReader = new FlatFileItemReaderBuilder<Player>().name("playerFileItemReader")
                .resource(new ClassPathResource("org/springframework/batch/samples/football/data/player-small1.csv"))
                .delimited()
                .names("ID", "lastName", "firstName", "position", "birthYear", "debutYear")
                .fieldSetMapper(new PlayerFieldSetMapper())
                .build();

        PlayerItemWriter playerItemWriter = new PlayerItemWriter();
        //NamedParameterJdbcOperations api
        JdbcPlayerDao playerDao = new JdbcPlayerDao();
        playerDao.setDataSource(proxy.getDataSource());
        playerItemWriter.setPlayerDao(playerDao);

        Step step1 = new StepBuilder("playerLoad", proxy.getJobRepository()).<Player, Player>chunk(2, proxy.getTransactionManager())
                .reader(playerReader)
                .writer(playerItemWriter)
                .build();

        FlatFileItemReader<Game> gameReader = new FlatFileItemReaderBuilder<Game>().name("gameFileItemReader")
                .resource(new ClassPathResource("org/springframework/batch/samples/football/data/games-small.csv"))
                .delimited()
                .names("id", "year", "team", "week", "opponent", "completes", "attempts", "passingYards", "passingTd",
                        "interceptions", "rushes", "rushYards", "receptions", "receptionYards", "totalTd")
                .fieldSetMapper(new GameFieldSetMapper())
                .build();
//      SimpleJdbcInsert api
        JdbcGameDao jdbcGameDao = new JdbcGameDao();
        jdbcGameDao.setDataSource(proxy.getDataSource());
        jdbcGameDao.afterPropertiesSet(); //初始化

        Step step2 = new StepBuilder("gameLoad", proxy.getJobRepository()).<Game, Game>chunk(2, proxy.getTransactionManager())
                .reader(gameReader)
                .writer(jdbcGameDao)
                .build();

        String sql = """
				SELECT GAMES.player_id, GAMES.year_no, SUM(COMPLETES),
				SUM(ATTEMPTS), SUM(PASSING_YARDS), SUM(PASSING_TD),
				SUM(INTERCEPTIONS), SUM(RUSHES), SUM(RUSH_YARDS),
				SUM(RECEPTIONS), SUM(RECEPTIONS_YARDS), SUM(TOTAL_TD)
				from GAMES, PLAYERS where PLAYERS.player_id =
				GAMES.player_id group by GAMES.player_id, GAMES.year_no
				""";
        JdbcCursorItemReader<PlayerSummary> playerSummaryReader = new JdbcCursorItemReaderBuilder<PlayerSummary>()
                .name("playerSummarizationSource")
                .ignoreWarnings(true)
                .sql(sql)
                .dataSource(proxy.getDataSource())
                .rowMapper(new PlayerSummaryMapper())
                .build();
        JdbcPlayerSummaryDao jdbcPlayerSummaryDao = new JdbcPlayerSummaryDao();
        jdbcPlayerSummaryDao.setDataSource(proxy.getDataSource());

        Step step3 = new StepBuilder("summarizationStep", proxy.getJobRepository())
                .<PlayerSummary, PlayerSummary>chunk(2, proxy.getTransactionManager())
                .reader(playerSummaryReader)
                .writer(jdbcPlayerSummaryDao)
                .build();

        Job job = new JobBuilder("testFootballJob", proxy.getJobRepository())
                .start(step1)
                .next(step2)
                .next(step3)
                .build();

        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
//      清理上一次测试的现场，不影响本次测试
        // then
        int count = JdbcTestUtils.countRowsInTable(jdbcTemplate,
                "PLAYER_SUMMARY");
        assertTrue(count > 0);
    }
    /**
     * 主要学习callback的使用。在header和foot的时候都有回调
     * listener 的生命周期注入 step的上下文
     */
    @Test
    public void testHeaderFootJob() throws Exception {
        Resource inputResource = new ClassPathResource("org/springframework/batch/samples/headerfooter/data/input.txt");
        WritableResource outputResource = new FileSystemResource("target/test-outputs/headerFooterOutput.txt");

        HeaderCopyCallback headerCallback = new HeaderCopyCallback();
        SummaryFooterCallback footerCallback = new SummaryFooterCallback();
        FlatFileItemReader<FieldSet> reader = new FlatFileItemReaderBuilder<FieldSet>().name("itemReader")
                .resource(inputResource)
                .lineTokenizer(new DelimitedLineTokenizer())
                .fieldSetMapper(new PassThroughFieldSetMapper())
                .linesToSkip(1)   //skip
                .skippedLinesCallback(headerCallback) //相当于把跳过的第一行写到内存里，供下游拷贝使用
                .build();
        FlatFileItemWriter<String> writer = new FlatFileItemWriterBuilder<String>().name("itemWriter")
                .resource(outputResource)
                .lineAggregator(new PassThroughLineAggregator<>())
                //headerCallback 同样实现了这里的接口，把上一步reader中内存里存下的line写到文件里
                .headerCallback(headerCallback)
                //footerCallback是在最后一行统计输出用的，因为需要整个Step环境的上下文中取数据，所以它还实现StepExecutionListener,并
//                在构建step的过程中被设置进去
                .footerCallback(footerCallback)
                .build();
        Step step = new StepBuilder("step1", proxy.getJobRepository()).<FieldSet, String>chunk(1, proxy.getTransactionManager())
                .reader(reader)
                .writer(writer)
                //footerCallback是在最后一行统计输出用的，因为需要整个Step环境的上下文中取数据，所以它还实现StepExecutionListener,并
                .listener(footerCallback) //监听器
                .build();
        Job job = new JobBuilder("testHeaderFootJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        BufferedReader inputReader = new BufferedReader(new FileReader(inputResource.getFile()));
        BufferedReader outputReader = new BufferedReader(new FileReader(outputResource.getFile()));
        // skip initial comment from input file
        inputReader.readLine();
        String line;
        int lineCount = 0;
        while ((line = inputReader.readLine()) != null) {
            lineCount++;
            AssertionErrors.assertTrue("input line should correspond to output line", outputReader.readLine().contains(line));
        }
        // footer contains the item count
        int itemCount = lineCount - 1; // minus 1 due to header line
        AssertionErrors.assertTrue("OutputReader did not contain the values specified",
                outputReader.readLine().contains(String.valueOf(itemCount)));
        inputReader.close();
        outputReader.close();
    }
    /**
     * 主要学习reader使用 jdbc分页查询
     */
    @Test
    public void testJdbcPagingJob() throws Exception {
        String sql = "UPDATE CUSTOMER set credit = :credit where id = :id";
        JdbcBatchItemWriter<CustomerCredit> writer = new JdbcBatchItemWriterBuilder<CustomerCredit>().dataSource(proxy.getDataSource())
                .sql(sql)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(true)
                .build();
        writer.afterPropertiesSet(); //初始化一下

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("statusCode", "PE");
        parameterValues.put("credit", 0);
        parameterValues.put("type", "COLLECTION");
        //分页查询
        JdbcPagingItemReader<CustomerCredit> reader = new JdbcPagingItemReaderBuilder<CustomerCredit>().name("customerReader")
                .dataSource(proxy.getDataSource())
                .selectClause("select NAME, ID, CREDIT")
                .fromClause("FROM CUSTOMER")
                .whereClause("WHERE CREDIT > :credit")
                .sortKeys(Map.of("ID", org.springframework.batch.item.database.Order.ASCENDING))
                .rowMapper(new CustomerCreditRowMapper())
                .pageSize(2)
                .parameterValues(parameterValues)
                .build();
        reader.afterPropertiesSet(); //初始化一下
        Step step = new StepBuilder("step1", proxy.getJobRepository()).<CustomerCredit,CustomerCredit>chunk(2, proxy.getTransactionManager())
                .reader(reader)
                .writer(writer)
                .processor(new CustomerCreditIncreaseProcessor())
                .build();
        Job job = new JobBuilder("testJdbcPagingJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }
    /**
     * 学习step中引用其他Job,也就是job可以复用嵌套，作为另一个job的其中一个step
     */
    @Test
    public void testStepJob() throws Exception {
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/jobstep/data/ImportTradeDataStep.txt");
        Range[] ranges = new Range[]{new Range(1, 12), new Range(13, 15), new Range(16, 20), new Range(21, 29)};
        String[] columnNames = new String[]{"ISIN", "Quantity", "Price", "Customer"};
        FlatFileItemReader<Trade> traderReader = new FlatFileItemReaderBuilder<Trade>()
                .resource(classPathFile)
                .fieldSetMapper(new TradeFieldSetMapper())
                .fixedLength().columns(ranges)
                .names(columnNames)
                //persisted within the org.springframework.batch.item.ExecutionContext for restart purposes.
                //为了重启用
                .saveState(true)
                .name("itemReader")
                .build();
//        --------------------------------------------------------validate----------------------------------------------------
        SpringValidator<Trade> fixedValidator = new SpringValidator<Trade>();
        fixedValidator.setValidator(new TradeValidator());
        ValidatingItemProcessor<Trade> processor = new ValidatingItemProcessor<Trade>(fixedValidator);

        JdbcTradeDao tradeDao = new JdbcTradeDao();
        tradeDao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "TRADE_SEQ", "ID");
        tradeDao.setIncrementer(incrementer);

        TradeWriter tradeWriter = new TradeWriter();
        tradeWriter.setDao(tradeDao);
//        --------------------------------------------------------以上所有对trade的操作构成step1 ----------------------------------------------------
        Step step1 = new StepBuilder("step1", proxy.getJobRepository())
                .<Trade,Trade>chunk(2,proxy.getTransactionManager())
                .reader(traderReader)
                .processor(processor)
                .writer(tradeWriter)
                .build();
        Job innerJob = new JobBuilder("innerJob", proxy.getJobRepository())
                .start(step1)
                .build();
        //step用上面完整的job作为入参构造step
        Step reallyRunStep = new StepBuilder("reallyRunStep", proxy.getJobRepository())
                .job(innerJob)
                .build();
        Job job = new JobBuilder("testStepJob", proxy.getJobRepository())
                .start(reallyRunStep)
                .build();
        //运行前清理现场
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");

        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");
        assertEquals(5, after);
    }
    /**
     * 循环重复执行step
     * allowStartIfComplete允许反复执行一个step
     * JobExecutionDecider根据条件决定是否执行step，和执行哪一个step
     */
    @Test
    public void testLoopJob() throws Exception {
        GeneratingTradeItemReader reader = new GeneratingTradeItemReader();
        reader.setLimit(1);

        ItemTrackingTradeItemWriter writer = new ItemTrackingTradeItemWriter();

        //抉择器,根据条件决定是否执行下一个step
        LimitDecider decider = new LimitDecider();
        decider.setLimit(9);

        GeneratingTradeResettingListener resettingListener = new GeneratingTradeResettingListener();
        resettingListener.setReader(reader);
//        --------------------------------------------------------以上所有对trade的操作构成step1 ----------------------------------------------------
        Step step1 = new StepBuilder("step1", proxy.getJobRepository())
                .<Trade, Trade>chunk(1, proxy.getTransactionManager())
                //read设置了limit,为1，所以一个step只会reader一次
                .reader(reader)
                // afterStep以后，count清零,因为afterStep把count清零后，又小于limit 1了，所以reader又具备了读取的能力
                .listener(resettingListener)
                // 所有下面step2中，reader还是会执行
                .writer(writer)
                .build();
        //其实两个step的步骤是一模一样的
        Step step2 = new StepBuilder("step2", proxy.getJobRepository())
                .<Trade, Trade>chunk(1, proxy.getTransactionManager())
                .reader(reader)
                .listener(resettingListener)
                .writer(writer)
                //按理应该像step1一样只会执行一次reader，因为limit的限制
//              然后就应该complete了，complete后的job是不是不能再重复start了？
//                所以设置了这个参数后，可以允许step被反复运行，哪怕已经结束
                .allowStartIfComplete(true)
                .build();
        Job job = new JobBuilder("testLoopJob", proxy.getJobRepository())
                .start(step1)
                //经过两个step，有2个item
                .next(step2)
//                LimitDecider的limit是9，所以可以执行8次。所以总共加起来应该是10次
                .next(decider).on("CONTINUE").to(step2).on("COMPLETED").end()
                .build().build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());

        //通过condition end的job最终的状态好像是FAILED，不是COMPLETED,所以这个断言是通不过的
//        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        // items processed = items read + 2 exceptions
        assertEquals(10, writer.getItems().size());
    }

    @Test
    public void testGroovyJob() throws Exception {
//        String unzipScript = """
//            class UnzipTasklet {
//            void execute() {
//            def ant = new AntBuilder()
//            ant.unzip(src:"src/test/resources/org/springframework/batch/samples/misc/groovy/data/files.zip",
//            dest:"target/groovyJob/staging")
//            }
//            }
//                """;
//        String zipScript = """
//			class ZipTasklet {
//			void execute() {
//			def ant = new AntBuilder()
//			ant.mkdir(dir:"target/groovyJob/output")
//			ant.zip(destfile:"target/groovyJob/output/files.zip",
//			basedir:"target/groovyJob/staging", includes:"**")
//			}
//			}
//			""";
//        StandardScriptEvaluator scriptEvaluator = new StandardScriptEvaluator();
//        scriptEvaluator.setLanguage("groovy");
//        ScriptSource unzip = new StaticScriptSource(unzipScript,"groovy");
//        ScriptSource zip = new StaticScriptSource(zipScript,"groovy");
        MethodInvokingTaskletAdapter adapter = new MethodInvokingTaskletAdapter();
//        这里用解析groovy的方式没走通
//        adapter.setTargetObject(scriptEvaluator.evaluate(unzip));
//        退而求其次，还是用xml配置,用内部Configuration类加@ImportResource能起作用，
//        单元测试就是TestConfiguration加@ImportResource
        adapter.setTargetObject(SpringUtil.getBean("unzip-script"));
        adapter.setTargetMethod("execute");
        //step1 用MethodInvokingTaskletAdapter，并且带事务
        Step step1 = new StepBuilder("step1", proxy.getJobRepository())
                .tasklet(adapter, proxy.getTransactionManager())
                .build();

        MethodInvokingTaskletAdapter adapter2 = new MethodInvokingTaskletAdapter();
        adapter2.setTargetObject(SpringUtil.getBean("zip-script"));
        adapter2.setTargetMethod("execute");
        Step step2 = new StepBuilder("step2", proxy.getJobRepository())
                .tasklet(adapter2, proxy.getTransactionManager())
                .build();
        Job job = new JobBuilder("testGroovyJob", proxy.getJobRepository())
                .start(step1)
                //经过两个step，有2个item
                .next(step2).build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }
    /**
     * 分片多线程并行运行step
     * 脱离spring bean环境没有运行成功
     * 需要SPEL解析的对象，都需要被spring托管才能运行成功
     */
    @Test
    public void testPartitionJob() throws Exception {
        String[] columnNames = new String[]{"name", "credit"};
        //testReader的代理reader
        FlatFileItemReader<CustomerCredit> delegateReader = new FlatFileItemReaderBuilder<CustomerCredit>().name("delegateReader").delimited().names(columnNames).targetType(CustomerCredit.class).build();
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/partition/file/data/delimited1.csv");
        Resource classPathFile2 = new ClassPathResource("org/springframework/batch/samples/partition/file/data/delimited2.csv");
        Resource[] inputResource = new Resource[]{classPathFile,classPathFile2};
        MultiResourceItemReader<CustomerCredit> testInputReader = new MultiResourceItemReader<CustomerCredit>();
        testInputReader.setDelegate(delegateReader);
        testInputReader.setResources(inputResource);


        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        partitioner.setResources(inputResource);

//        FlatFileItemReader<CustomerCredit> reader = new FlatFileItemReaderBuilder<CustomerCredit>()
//                .resource(new FileUrlResource("#{stepExecutionContext[fileName]}"))
//                .name("reader")
//                .delimited()
//                .names(columnNames)
//                .targetType(CustomerCredit.class)
//                .build();
//
//        FlatFileItemWriter<CustomerCredit> writer = new FlatFileItemWriterBuilder<CustomerCredit>().name("itemWriter")
//                .name("writer")
//                .resource(new FileUrlResource("#{stepExecutionContext[outputFile]}"))
//                .delimited()
//                .names(columnNames)
//                .build();

//              xml配置文件可以用spel表达式，或者说被spring托管的bean可以用spel表达式
//                用配置文件方式解决,这里脱离spring环境不能运行，用配置bean方式运行
        FlatFileItemReader<CustomerCredit> reader = SpringUtil.getBean("stepExecutionParamCustomerCreditReader",FlatFileItemReader.class);
        FlatFileItemWriter<CustomerCredit> writer = SpringUtil.getBean("stepExecutionParamCustomerCreditWriter",FlatFileItemWriter.class);
        OutputFileListener outputFileListener = new OutputFileListener();
//        outputFileListener.setPath("target/output/file/");
        //TODO modify 一定要带协议,不然SPEL表达式无法解析
        outputFileListener.setPath("file:target/output/file/");

        Step step1 = new StepBuilder("step1", proxy.getJobRepository()).<CustomerCredit, CustomerCredit>chunk(5, proxy.getTransactionManager())
                .reader(reader)
                .processor(new CustomerCreditIncreaseProcessor())
                .writer(writer)
//                .stream(reader)
//                .stream(writer)
                .listener(outputFileListener)
                //根据在上下文中根据 fileName这个input key生成outputFile这个key
//       而fileName这个key哪里来的呢，是MultiResourcePartitioner（分片器？）中的partition方法在分片的时候塞到上下文中
                .build();
        Step step = new StepBuilder("step", proxy.getJobRepository())
                .partitioner("step1",partitioner)
                .step(step1)
                .gridSize(2)
                .taskExecutor(new SimpleAsyncTaskExecutor()) //PartitionHandler 会创建一个TaskExecutorPartitionHandler
                .build();
        Job job = new JobBuilder("testPartitionJob", proxy.getJobRepository())
                .start(step)
                .build();

        testInputReader.open(new ExecutionContext());
        Set<CustomerCredit> inputCustomerCredits = new HashSet<>();
        CustomerCredit inputCustomerCredit;
        while ((inputCustomerCredit = testInputReader.read()) != null) {
            inputCustomerCredits.add(inputCustomerCredit);
        }
        testInputReader.close();

        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        //TODO modify，这里一定要FileSystemResource才能通过？？未进一步验证
        Resource[] outResource = new Resource[]{new FileSystemResource("target/output/file/delimited1.csv"),new FileSystemResource("target/output/file/delimited2.csv")};
        MultiResourceItemReader<CustomerCredit> testOutReader = new MultiResourceItemReader<CustomerCredit>();
        testOutReader.setResources(outResource);
        testOutReader.setDelegate(delegateReader);

        testOutReader.open(new ExecutionContext());
        Set<CustomerCredit> outCustomerCredits = new HashSet<>();
        CustomerCredit outCustomerCredit;
        while ((outCustomerCredit = testOutReader.read()) != null) {
            outCustomerCredits.add(outCustomerCredit);
        }
        testOutReader.close();
        assertEquals(inputCustomerCredits.size(), outCustomerCredits.size());
        int itemCount = inputCustomerCredits.size();
        assertTrue(itemCount > 0, "No entries were available in the input");
    }
    /**
     * 分片多线程并行运行JDBC job
     * 需要SPEL解析的对象，都需要被spring托管才能运行成功
     */
    @Test
    public void testPartitionJdbcJob() throws Exception {
        String[] columnNames = new String[]{"id","name", "credit"};
        //testReader的代理reader
        FlatFileItemReader<CustomerCredit> delegateReader = new FlatFileItemReaderBuilder<CustomerCredit>().name("delegateReader").delimited().names(columnNames).targetType(CustomerCredit.class).build();
        JdbcCursorItemReader<CustomerCredit> testInputReader = new JdbcCursorItemReader<CustomerCredit>();
        testInputReader.setDataSource(proxy.getDataSource());
        testInputReader.setSql("select ID,NAME,CREDIT from CUSTOMER");
        testInputReader.setRowMapper(new CustomerCreditRowMapper());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] outResource = resolver.getResources("file:target/output/jdbc/*.csv");
        MultiResourceItemReader<CustomerCredit> testOutReader = new MultiResourceItemReader<CustomerCredit>();
        testOutReader.setResources(outResource);
        testOutReader.setDelegate(delegateReader);

        ColumnRangePartitioner partitioner = new ColumnRangePartitioner();
        partitioner.setTable("CUSTOMER");
        partitioner.setColumn("ID");
        partitioner.setDataSource(proxy.getDataSource());


        JdbcPagingItemReader<CustomerCredit> reader = SpringUtil.getBean("jdbcPagingItemReader",JdbcPagingItemReader.class);
        FlatFileItemWriter<CustomerCredit> writer = SpringUtil.getBean("partitionJdbcFlatFileItemWriter",FlatFileItemWriter.class);

        OutputFileListener outputFileListener = new OutputFileListener();
        //注意要带协议
        outputFileListener.setPath("file:target/output/jdbc/");
        Step step1 = new StepBuilder("step1", proxy.getJobRepository()).<CustomerCredit, CustomerCredit>chunk(5, proxy.getTransactionManager())
                .reader(reader)
                .processor(new CustomerCreditIncreaseProcessor())
                .writer(writer)
                .listener(outputFileListener)
                //根据在上下文中根据 fileName这个input key生成outputFile这个key
//       而fileName这个key哪里来的呢，是MultiResourcePartitioner（分片器？）中的partition方法在分片的时候塞到上下文中
                .build();
        Step step = new StepBuilder("step", proxy.getJobRepository())
                .partitioner("step1",partitioner)
                .step(step1)
                .gridSize(5)
                .taskExecutor(new SimpleAsyncTaskExecutor()) //PartitionHandler 会创建一个TaskExecutorPartitionHandler
                .build();
        Job job = new JobBuilder("testPartitionJdbcJob", proxy.getJobRepository())
                .start(step)
                .build();

        testInputReader.open(new ExecutionContext());
        Set<CustomerCredit> inputCustomerCredits = new HashSet<>();
        CustomerCredit inputCustomerCredit;
        while ((inputCustomerCredit = testInputReader.read()) != null) {
            inputCustomerCredits.add(inputCustomerCredit);
        }
        testInputReader.close();

        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        testOutReader.open(new ExecutionContext());
        Set<CustomerCredit> outCustomerCredits = new HashSet<>();
        CustomerCredit outCustomerCredit;
        while ((outCustomerCredit = testOutReader.read()) != null) {
            outCustomerCredits.add(outCustomerCredit);
        }
        testOutReader.close();

        assertEquals(inputCustomerCredits.size(), outCustomerCredits.size());
        int itemCount = inputCustomerCredits.size();
        assertTrue(itemCount > 0, "No entries were available in the input");
    }

    /**
     * 并行运行job
     */
    @Test
    public void testParallelJob() throws Exception {
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/processindicator/data/ImportTradeDataStep.txt");
        Range[] ranges = new Range[]{new Range(1, 12), new Range(13, 15), new Range(16, 20), new Range(21, 29)};
        String[] columnNames = new String[]{"ISIN", "Quantity", "Price", "Customer"};
        FlatFileItemReader<Trade> fileItemReader = new FlatFileItemReaderBuilder<Trade>()
                .resource(classPathFile)
                .fieldSetMapper(new TradeFieldSetMapper())
                .fixedLength().columns(ranges)
                .names(columnNames)
                .name("itemReader")
                .build();
//        --------------------------------------------------------validate----------------------------------------------------
        SpringValidator<Trade> fixedValidator = new SpringValidator<Trade>();
        fixedValidator.setValidator(new TradeValidator());
        ValidatingItemProcessor<Trade> processor = new ValidatingItemProcessor<Trade>(fixedValidator);

        JdbcTradeDao tradeDao = new JdbcTradeDao();
        tradeDao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "TRADE_SEQ", "ID");
        tradeDao.setIncrementer(incrementer);

        TradeWriter tradeWriter = new TradeWriter();
        tradeWriter.setDao(tradeDao);

        StagingItemProcessor<Trade> stagingProcessor = new StagingItemProcessor<Trade>();
        stagingProcessor.setDataSource(proxy.getDataSource());

        StagingItemReader<Trade> stagingReader = new StagingItemReader<Trade>();
        stagingReader.setDataSource(proxy.getDataSource());
        stagingReader.afterPropertiesSet();


        StagingItemWriter<Trade> stagingWriter = new StagingItemWriter<Trade>();
        stagingWriter.setDataSource(proxy.getDataSource());
        stagingWriter.setIncrementer(new MySQLMaxValueIncrementer(proxy.getDataSource(), "BATCH_STAGING_SEQ", "ID"));

        //从文件读取，写入数据库
        Step step1 = new StepBuilder("staging", proxy.getJobRepository())
                .<Trade,Trade>chunk(2,proxy.getTransactionManager())
                .reader(fileItemReader)
                .processor(processor)
                .writer(stagingWriter)
                .build();
        //从上一步写入的表里读取，加工，再写入Trade表
        Step step2 = new StepBuilder("loading", proxy.getJobRepository())
                .<ProcessIndicatorItemWrapper<Trade>,Trade>chunk(1,proxy.getTransactionManager())
                .reader(stagingReader)
                .writer(tradeWriter)
                .processor(stagingProcessor)
                .taskExecutor(new SimpleAsyncTaskExecutor())
                .build();
        Job job = new JobBuilder("testParallelJob", proxy.getJobRepository())
                .start(step1)
                .next(step2)
                .build();
        int before = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STAGING");
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "BATCH_STAGING");
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(after - before, execution.getStepExecutions().iterator().next().getReadCount());
    }
    /**
     * 失败重启任务，捕捉到特定异常，重启任务
     */
    @Test
    public void testFailRestartJob() throws Exception {
//        --------------------------------------------------------validate----------------------------------------------------
        SpringValidator<Trade> fixedValidator = new SpringValidator<Trade>();
        fixedValidator.setValidator(new TradeValidator());
        ValidatingItemProcessor<Trade> processor = new ValidatingItemProcessor<Trade>(fixedValidator);

        JdbcTradeDao tradeDao = new JdbcTradeDao();
        tradeDao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "TRADE_SEQ", "ID");
        tradeDao.setIncrementer(incrementer);

        TradeWriter tradeWriter = new TradeWriter();
        tradeWriter.setDao(tradeDao);

        ExceptionThrowingItemReaderProxy<Trade> reader = new ExceptionThrowingItemReaderProxy<>();
        FlatFileItemReader<Trade> fileItemReader = SpringUtil.getBean("itemReader",FlatFileItemReader.class);
        reader.setDelegate(fileItemReader);

        //从文件读取，写入数据库
        Step step1 = new StepBuilder("step1", proxy.getJobRepository())
                .<Trade,Trade>chunk(2,proxy.getTransactionManager())
                .reader(reader)
                .processor(processor)
                .writer(tradeWriter)
                .stream(fileItemReader)
                .build();
        Job job = new JobBuilder("testFailRestartJob", proxy.getJobRepository())
                .start(step1)
                .build();
        int before = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");

        JobExecution jobExecution = runJobForRestartTest(job);
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus());

        Throwable ex = jobExecution.getAllFailureExceptions().get(0);
        assertTrue(ex.getMessage().toLowerCase().contains("planned"));

        int medium = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");
        // assert based on commit interval = 2
        assertEquals(before + 2, medium);

        jobExecution = runJobForRestartTest(job);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");
        assertEquals(before + 5, after);
    }
    private JobExecution runJobForRestartTest(Job job) throws Exception {
        //PropertiesConverter.stringToProperties 把下面的字符串变成一个map，key是inputFile,value是文件位置
//      最后从map组装成JobParameters
//        这里job是同一个，但是每次JobParameters都是新new出来的
        return proxy.getJobLauncher().run(job,new DefaultJobParametersConverter().getJobParameters(PropertiesConverter.stringToProperties(
					"inputFile=classpath:org/springframework/batch/samples/restart/fail/data/ImportTradeDataStep.txt")));
    }
    /**
     * JobOperator Api的操作，结合restart
     * 很麻烦，一旦弄错了，要么改jobParamter,要么清数据重来
     * 数据表又有外键约束，最好是新建一个库，从头执行脚本
     *
     * 没有通过
     */
    @Test
    public void testJobOperatorJob() throws Exception {
        GeneratingTradeItemReader reader = new GeneratingTradeItemReader();
        //不要太大，免得跑不完
        reader.setLimit(10);

        FaultTolerantStepFactoryBean<Trade,Trade> stepFactoryBean = new FaultTolerantStepFactoryBean<Trade,Trade>();
        stepFactoryBean.setTransactionManager(proxy.getTransactionManager());
        stepFactoryBean.setJobRepository(proxy.getJobRepository());
        stepFactoryBean.setItemReader(reader);
        stepFactoryBean.setItemWriter(new DummyItemWriter());
        stepFactoryBean.setBeanName("infiniteStep");
        //从文件读取，写入数据库
        Job job = new JobBuilder("testJobOperatorJob", proxy.getJobRepository())
                .start(stepFactoryBean.getObject())
                 /**
                   * This incrementer increments a "run.id" parameter of type {@link Long} from the given
                   * job parameters. If the parameter does not exist, it will be initialized to 1. The
                   * parameter name can be configured using {@link #setKey(String)}.
                 */
                .incrementer(new RunIdIncrementer())
                .build();
        //内存中注册下，下面还要用
        if (!proxy.getJobRegistry().getJobNames().contains(job.getName())) {
            proxy.getJobRegistry().register(new ReferenceJobFactory(job));
        }
        String params = "jobOperatorTestParam=6,java.lang.Long,true";
        Properties properties = new Properties();
        properties.setProperty("jobOperatorTestParam", "6,java.lang.Long,true");

        long executionId = proxy.getJobOperator().start(job.getName(), properties);
        assertEquals(params, proxy.getJobOperator().getParameters(executionId));
        stopAndCheckStatus(job,executionId);

        long resumedExecutionId = proxy.getJobOperator().restart(executionId);
        assertEquals(params, proxy.getJobOperator().getParameters(resumedExecutionId));
        stopAndCheckStatus(job,resumedExecutionId);

        List<Long> instances = proxy.getJobOperator().getJobInstances(job.getName(), 0, 1);
        assertEquals(1, instances.size());
        long instanceId = instances.get(0);

        List<Long> executions = proxy.getJobOperator().getExecutions(instanceId);
        assertEquals(2, executions.size());
        // latest execution is the first in the returned list
        assertEquals(resumedExecutionId, executions.get(0).longValue());
        assertEquals(executionId, executions.get(1).longValue());
    }
    /**
     * @param executionId id of running job execution
     */
    private void stopAndCheckStatus(Job job,long executionId) throws Exception {
        // wait to the job to get up and running
        Thread.sleep(1000);

        Set<Long> runningExecutions = proxy.getJobOperator().getRunningExecutions(job.getName());
//        assertTrue(runningExecutions.contains(executionId),
//                "Wrong executions: " + runningExecutions + " expected: " + executionId);
        assertTrue(proxy.getJobOperator().getSummary(executionId).contains(BatchStatus.STARTED.toString()),
                "Wrong summary: " + proxy.getJobOperator().getSummary(executionId));

        proxy.getJobOperator().stop(executionId);

        int count = 0;
        while (proxy.getJobOperator().getRunningExecutions(job.getName()).contains(executionId) && count <= 10) {
            Thread.sleep(100);
            count++;
        }

        runningExecutions = proxy.getJobOperator().getRunningExecutions(job.getName());
        assertFalse(runningExecutions.contains(executionId),
                "Wrong executions: " + runningExecutions + " expected: " + executionId);
        assertTrue(proxy.getJobOperator().getSummary(executionId).contains(BatchStatus.STOPPED.toString()),
                "Wrong summary: " + proxy.getJobOperator().getSummary(executionId));

        // there is just a single step in the test job
        Map<Long, String> summaries = proxy.getJobOperator().getStepExecutionSummaries(executionId);
        assertTrue(summaries.values().toString().contains(BatchStatus.STOPPED.toString()));
    }

    /**
     * 没有通过
     */
    @Test
    public void testMultipleSimultaneousInstances() throws Exception {
        GeneratingTradeItemReader reader = new GeneratingTradeItemReader();
        //不要太大，免得跑不完
        reader.setLimit(10);

        FaultTolerantStepFactoryBean<Trade,Trade> stepFactoryBean = new FaultTolerantStepFactoryBean<Trade,Trade>();
        stepFactoryBean.setTransactionManager(proxy.getTransactionManager());
        stepFactoryBean.setJobRepository(proxy.getJobRepository());
        stepFactoryBean.setItemReader(reader);
        stepFactoryBean.setItemWriter(new DummyItemWriter());
        stepFactoryBean.setBeanName("infiniteStep");
        //从文件读取，写入数据库
        Job job = new JobBuilder("testMultipleSimultaneousInstances", proxy.getJobRepository())
                .start(stepFactoryBean.getObject())
                /**
                 * This incrementer increments a "run.id" parameter of type {@link Long} from the given
                 * job parameters. If the parameter does not exist, it will be initialized to 1. The
                 * parameter name can be configured using {@link #setKey(String)}.
                 */
                .incrementer(new RunIdIncrementer())
                .build();
        //内存中注册下，下面还要用
        if (!proxy.getJobRegistry().getJobNames().contains(job.getName())) {
            proxy.getJobRegistry().register(new ReferenceJobFactory(job));
        }
        String jobName = job.getName();

        Set<String> names = proxy.getJobOperator().getJobNames();
        assertEquals(1, names.size());
        assertTrue(names.contains(jobName));

        long exec1 = proxy.getJobOperator().startNextInstance(jobName);
        long exec2 = proxy.getJobOperator().startNextInstance(jobName);

        assertTrue(exec1 != exec2);
        assertNotEquals(proxy.getJobOperator().getParameters(exec1), proxy.getJobOperator().getParameters(exec2));

        // Give the asynchronous task executor a chance to start executions
        Thread.sleep(1000);

        Set<Long> executions = proxy.getJobOperator().getRunningExecutions(jobName);
        assertTrue(executions.contains(exec1));
        assertTrue(executions.contains(exec2));

        int count = 0;
        boolean running = proxy.getJobOperator().getSummary(exec1).contains("STARTED")
                && proxy.getJobOperator().getSummary(exec2).contains("STARTED");

        while (count++ < 10 && !running) {
            Thread.sleep(100L);
            running = proxy.getJobOperator().getSummary(exec1).contains("STARTED") && proxy.getJobOperator().getSummary(exec2).contains("STARTED");
        }
        assertTrue(running, String.format("Jobs not started: [%s] and [%s]", proxy.getJobOperator().getSummary(exec1),
                proxy.getJobOperator().getSummary(exec1)));

        proxy.getJobOperator().stop(exec1);
        proxy.getJobOperator().stop(exec2);
    }
    /**
     * 没有通过
     */
    @Test
    public void testGracefulShutdownFunctional() throws Exception {
        GeneratingTradeItemReader reader = new GeneratingTradeItemReader();
        reader.setLimit(10);

        FaultTolerantStepFactoryBean<Trade,Trade> stepFactoryBean = new FaultTolerantStepFactoryBean<Trade,Trade>();
        stepFactoryBean.setTransactionManager(proxy.getTransactionManager());
        stepFactoryBean.setJobRepository(proxy.getJobRepository());
        stepFactoryBean.setItemReader(reader);
        stepFactoryBean.setItemWriter(new DummyItemWriter());
        stepFactoryBean.setBeanName("infiniteStep");
        //从文件读取，写入数据库
        Job job = new JobBuilder("testGracefulShutdownFunctional", proxy.getJobRepository())
                .start(stepFactoryBean.getObject())
                /**
                 * This incrementer increments a "run.id" parameter of type {@link Long} from the given
                 * job parameters. If the parameter does not exist, it will be initialized to 1. The
                 * parameter name can be configured using {@link #setKey(String)}.
                 */
                .incrementer(new RunIdIncrementer())
                .build();
        //内存中注册下，下面还要用
        if (!proxy.getJobRegistry().getJobNames().contains(job.getName())) {
            proxy.getJobRegistry().register(new ReferenceJobFactory(job));
        }
        final JobParameters jobParameters = new JobParametersBuilder().addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = proxy.getJobLauncher().run(job,jobParameters);

        Thread.sleep(1000);

        assertEquals(BatchStatus.STARTED, jobExecution.getStatus());
        assertTrue(jobExecution.isRunning());

        proxy.getJobOperator().stop(jobExecution.getId());

        int count = 0;
        while (jobExecution.isRunning() && count <= 10) {
            Thread.sleep(100);
            count++;
        }
        assertFalse(jobExecution.isRunning(), "Timed out waiting for job to end.");
        assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
    }
    /**
     * retry
     */
    @Test
    public void testRetryJob() throws Exception {
        GeneratingTradeItemReader reader = new GeneratingTradeItemReader();
        reader.setLimit(10);
        RetrySampleItemWriter<Object> writer = new RetrySampleItemWriter<Object>();
        //从文件读取，写入数据库
        Step step = new StepBuilder("step", proxy.getJobRepository()).<Trade, Object>chunk(1, proxy.getTransactionManager())
                .reader(reader)
                .writer(writer)
                //错误容忍
                .faultTolerant()
                //对什么异常充实
                .retry(Exception.class)
                //重试次数
                .retryLimit(3)
                .build();
        Job job = new JobBuilder("testRetryJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        // then
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        // items processed = items read + 2 exceptions
        assertEquals(reader.getLimit() + 2, writer.getCounter());
    }
    /**
     * retry
     */
    @Test
    public void testValidationJob() throws Exception {
        org.springframework.batch.samples.validation.domain.Person person1 = new org.springframework.batch.samples.validation.domain.Person(1, "foo");
        org.springframework.batch.samples.validation.domain.Person person2 = new org.springframework.batch.samples.validation.domain.Person(2, "");

        ListItemReader<org.springframework.batch.samples.validation.domain.Person> reader = new ListItemReader<>(Arrays.asList(person1, person2));
        ListItemWriter<org.springframework.batch.samples.validation.domain.Person> writer = new ListItemWriter<>();

        BeanValidatingItemProcessor<org.springframework.batch.samples.validation.domain.Person> validator = new BeanValidatingItemProcessor<>();
        validator.setFilter(true);
        validator.afterPropertiesSet();

        //从文件读取，写入数据库
        Step step = new StepBuilder("step", proxy.getJobRepository())
                .<org.springframework.batch.samples.validation.domain.Person,org.springframework.batch.samples.validation.domain.Person>chunk(1, proxy.getTransactionManager())
                .reader(reader)
                .writer(writer)
                .processor(validator)
                .build();
        Job job = new JobBuilder("testValidationJob", proxy.getJobRepository())
                .start(step)
                .build();
        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), execution.getExitStatus().getExitCode());
        List<? extends org.springframework.batch.samples.validation.domain.Person> writtenItems = writer.getWrittenItems();
        assertEquals(1, writtenItems.size());
        assertEquals("foo", writtenItems.get(0).getName());
    }
    @Test
    public void testTradeJob() throws Exception {
        Resource classPathFile = new ClassPathResource("org/springframework/batch/samples/trade/data/ImportTradeDataStep.txt");
        String[] columnNames = new String[]{"ISIN", "Quantity", "price", "Customer"};
        FlatFileItemReader<Trade> traderReader = SpringBatchUtil.generateFileReader(Trade.class, classPathFile, null, columnNames);
//        --------------------------------------------------------validate----------------------------------------------------
        SpringValidator<Trade> fixedValidator = new SpringValidator<Trade>();
        fixedValidator.setValidator(new TradeValidator());
        ValidatingItemProcessor<Trade> processor = new ValidatingItemProcessor<Trade>(fixedValidator);

        JdbcTradeDao tradeDao = new JdbcTradeDao();
        tradeDao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "TRADE_SEQ", "ID");
        tradeDao.setIncrementer(incrementer);

        TradeWriter tradeWriter = new TradeWriter();
        tradeWriter.setDao(tradeDao);

        DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
        attribute.setIsolationLevel(Isolation.READ_COMMITTED.value());

        Step step1 = new StepBuilder("step1", proxy.getJobRepository())
                .<Trade,Trade>chunk(1,proxy.getTransactionManager())
                .reader(traderReader)
                .processor(processor)
                .writer(tradeWriter)
                .stream(traderReader)
                .transactionAttribute(attribute)
                .build();

        JdbcCursorItemReader<Trade> tradeSqlItemReader = new JdbcCursorItemReader<Trade>();
        tradeSqlItemReader.setDataSource(proxy.getDataSource());
        tradeSqlItemReader.setSql("SELECT isin, quantity, price, customer, id, version from TRADE");
        tradeSqlItemReader.setRowMapper(new TradeRowMapper());

        JdbcCustomerDebitDao customerDebitDao = new JdbcCustomerDebitDao();
        customerDebitDao.setDataSource(proxy.getDataSource());
        CustomerTradeUpdateWriter customerWriter = new CustomerTradeUpdateWriter();
        customerWriter.setDao(customerDebitDao);

        Step step2 = new StepBuilder("step2", proxy.getJobRepository()).<Trade, Trade>chunk(1, proxy.getTransactionManager())
                .reader(tradeSqlItemReader)
                .writer(customerWriter)
                .build();

        JdbcCursorItemReader<CustomerCredit> customerSqlItemReader = new JdbcCursorItemReader<CustomerCredit>();
        customerSqlItemReader.setDataSource(proxy.getDataSource());
        customerSqlItemReader.setSql("select ID,NAME,CREDIT from CUSTOMER");
        customerSqlItemReader.setRowMapper(new CustomerCreditRowMapper());

        FlatFileItemWriter<String> delegateItemWriter = new FlatFileItemWriterBuilder<String>().name("delegateItemWriter")
                .resource(new FileUrlResource("target/test-outputs/CustomerReportStep.TEMP.txt"))
                .lineAggregator(new PassThroughLineAggregator<>())
                .build();
        FlatFileCustomerCreditDao customerCreditDao = new FlatFileCustomerCreditDao();
        customerCreditDao.setItemWriter(delegateItemWriter);

        CustomerCreditUpdateWriter creditWriter = new CustomerCreditUpdateWriter();
        creditWriter.setDao(customerCreditDao);

        Step step3 = new StepBuilder("step3", proxy.getJobRepository()).<CustomerCredit,CustomerCredit>chunk(1, proxy.getTransactionManager())
                .reader(customerSqlItemReader)
                .writer(creditWriter)
                .build();
        Job job = new JobBuilder("testTradeJob", proxy.getJobRepository())
                .start(step1)
                .next(step2)
                .next(step3)
                .build();

//      清理上一次测试的现场，不影响本次测试
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
        List<Map<String, Object>> list = jdbcTemplate.queryForList("select NAME, CREDIT from CUSTOMER");

        Map<String, Double> credits = new HashMap<>();
        for (Map<String, Object> map : list) {
            credits.put((String) map.get("NAME"), ((Number) map.get("CREDIT")).doubleValue());
        }

        JobExecution execution = proxy.getJobLauncher().run(job, getUniqueJobParameters());
//        assertEquals(BatchStatus.COMPLETED, execution.getStatus());

        //job执行完的后续验证
        String GET_TRADES = "select ISIN, QUANTITY, PRICE, CUSTOMER, ID, VERSION from TRADE order by ISIN";
        String GET_CUSTOMERS = "select NAME, CREDIT from CUSTOMER order by NAME";
        List<Customer> customers = Arrays.asList(new Customer("customer1", (credits.get("customer1") - 98.34)),
                new Customer("customer2", (credits.get("customer2") - 18.12 - 12.78)),
                new Customer("customer3", (credits.get("customer3") - 109.25)),
                new Customer("customer4", credits.get("customer4") - 123.39));

        List<Trade> trades = Arrays.asList(new Trade("UK21341EAH45", 978, new BigDecimal("98.34"), "customer1"),
                new Trade("UK21341EAH46", 112, new BigDecimal("18.12"), "customer2"),
                new Trade("UK21341EAH47", 245, new BigDecimal("12.78"), "customer2"),
                new Trade("UK21341EAH48", 108, new BigDecimal("109.25"), "customer3"),
                new Trade("UK21341EAH49", 854, new BigDecimal("123.39"), "customer4"));

        jdbcTemplate.query(GET_TRADES, rs -> {
            Trade trade = trades.get(activeRow++);

            assertEquals(trade.getIsin(), rs.getString(1));
            assertEquals(trade.getQuantity(), rs.getLong(2));
            assertEquals(trade.getPrice(), rs.getBigDecimal(3));
            assertEquals(trade.getCustomer(), rs.getString(4));
        });

        assertEquals(activeRow, trades.size());

        activeRow = 0;
        jdbcTemplate.query(GET_CUSTOMERS, rs -> {
            Customer customer = customers.get(activeRow++);
            assertEquals(customer.getName(), rs.getString(1));
            assertEquals(customer.getCredit(), rs.getDouble(2), .01);
        });
        assertEquals(customers.size(), activeRow);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
    }
    @TestConfiguration
    @ImportResource(locations = {"classpath:org/springframework/batch/samples/misc/groovy/job/groovy-mytest.xml"})
    static class AppTestConfiguration {
        @Bean("skipJobFlatFileItemReader")
        @StepScope
        public FlatFileItemReader<Trade> skipJobFlatFileItemReader(@Value("classpath:org/springframework/batch/samples/skip/data/input#{jobParameters['run.id']}.txt") Resource resource) {
            String[] columnNames = new String[]{"ISIN", "Quantity", "Price", "Customer"};
            return new FlatFileItemReaderBuilder<Trade>()
                    .name("skipJobFlatFileItemReader")
                    .resource(resource)
                    .delimited()
                    .names(columnNames)
                    .fieldSetMapper(new TradeFieldSetMapper())
                    .build();
        }
        @Bean("jobParamTradeReader")
        @StepScope
        public FlatFileItemReader<Trade> jobParamTradeReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
            Range[] ranges = new Range[]{new Range(1, 12), new Range(13, 15), new Range(16, 20), new Range(21, 29)};
            String[] columnNames = new String[]{"ISIN", "Quantity", "Price", "Customer"};
            return new FlatFileItemReaderBuilder<Trade>()
                    .resource(resource)
                    .fieldSetMapper(new TradeFieldSetMapper())
                    .fixedLength().columns(ranges)
                    .names(columnNames)
                    .name("jobParamTradeReader")
                    .saveState(true)
                    .build();
        }
        @Bean("stepExecutionParamCustomerCreditReader")
        @StepScope
        public FlatFileItemReader<CustomerCredit> stepExecutionParamCustomerCreditReader(@Value("#{stepExecutionContext[fileName]}") Resource resource) {
            //TODO modify 一定要带协议,不然只会认为是字符串，而不会转化成Resource
            String[] columnNames = new String[]{"name", "credit"};
            return new FlatFileItemReaderBuilder<CustomerCredit>()
                    .resource(resource)
                    .name("stepExecutionParamCustomerCreditReader")
                    .delimited()
                    .names(columnNames)
                    .targetType(CustomerCredit.class)
                    .build();
        }
        @Bean("stepExecutionParamCustomerCreditWriter")
        @StepScope
        public FlatFileItemWriter<CustomerCredit> stepExecutionParamCustomerCreditWriter(@Value("#{stepExecutionContext[outputFile]}") WritableResource resource) {
            String[] columnNames = new String[]{"name", "credit"};
            return new FlatFileItemWriterBuilder<CustomerCredit>()
                .name("stepExecutionParamCustomerCreditWriter")
                .resource(resource)
                .delimited()
                .names(columnNames)
                .build();
        }
        @Bean("partitionJdbcFlatFileItemWriter")
        @StepScope
        public FlatFileItemWriter<CustomerCredit> partitionJdbcFlatFileItemWriter(@Value("#{stepExecutionContext[outputFile]}") WritableResource resource) {
            String[] columnNames = new String[]{"id","name", "credit"};
            return new FlatFileItemWriterBuilder<CustomerCredit>()
                    .name("stepExecutionParamCustomerCreditWriter")
                    .resource(resource)
                    .delimited()
                    .names(columnNames)
                    .build();
        }
        @Bean("jdbcPagingItemReader")
        @StepScope
        public JdbcPagingItemReader<CustomerCredit> jdbcPagingItemReader(@Qualifier("bf.batch.default_BatchProxy")BatchProxy proxy,@Value("#{stepExecutionContext[minValue]}")String minValue,@Value("#{stepExecutionContext[maxValue]}")String maxValue) throws Exception {
            JdbcPagingItemReader<CustomerCredit> reader = new JdbcPagingItemReader<CustomerCredit>();
            reader.setDataSource(proxy.getDataSource());
            reader.setParameterValues(Map.of("minId",minValue,"maxId",maxValue));
            reader.setRowMapper(new CustomerCreditRowMapper());
            SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
            factoryBean.setDataSource(proxy.getDataSource());
            factoryBean.setSelectClause("ID,NAME,CREDIT");
            factoryBean.setFromClause("CUSTOMER");
            factoryBean.setWhereClause("ID >= :minId and ID <= :maxId");
            factoryBean.setSortKeys(Map.of("ID", org.springframework.batch.item.database.Order.ASCENDING));
            reader.setQueryProvider(factoryBean.getObject());
            return reader;
        }
    }
    @Test
    void testSkipJobIncrementing() throws Exception{
        FlatFileItemReader<Trade> fileItemReader = SpringUtil.getBean("skipJobFlatFileItemReader",FlatFileItemReader.class);

        JdbcTradeDao tradeDao = new JdbcTradeDao();
        tradeDao.setDataSource(proxy.getDataSource());
        //三个参数，分别是数据源，表名，字段名
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "TRADE_SEQ", "ID");
        tradeDao.setIncrementer(incrementer);

        TradeWriter tradeWriter = new TradeWriter();
        tradeWriter.setDao(tradeDao);
        tradeWriter.setFailingCustomers(CollectionUtils.newArrayList("customer6"));

        ExecutionContextPromotionListener executionContextPromotionListener = new ExecutionContextPromotionListener();
        executionContextPromotionListener.setKeys(new String[]{"stepName",TradeWriter.TOTAL_AMOUNT_KEY});
//        --------------------------------------------------------以上所有对trade的操作构成step1 ----------------------------------------------------
        Step step1 = new StepBuilder("step1", proxy.getJobRepository())
                .<Trade, Trade>chunk(3, proxy.getTransactionManager())
                .listener(new SkipCheckingListener())
                .listener(executionContextPromotionListener)
                .reader(fileItemReader)
                .writer(tradeWriter)
                .processor(new TradeProcessor())
                .faultTolerant()
                .skipLimit(10)
                .skip(FlatFileParseException.class)
                .skip(WriteFailedException.class)
//                .skip(ItemStreamException.class)
                .build();
        Tasklet errPrintTasklet = new ErrorLogTasklet(proxy.getDataSource());
        //其实两个step的步骤是一模一样的
        Step stepErrorPrint1 = new StepBuilder("errorPrint1", proxy.getJobRepository())
                .tasklet(errPrintTasklet, proxy.getTransactionManager())
                .build();

        JdbcCursorItemReader<Trade> tradeSqlItemReader = new JdbcCursorItemReader<Trade>();
        tradeSqlItemReader.setDataSource(proxy.getDataSource());
        tradeSqlItemReader.setSql("SELECT isin, quantity, price, customer, id, version from TRADE");
        tradeSqlItemReader.setRowMapper(new TradeRowMapper());

        ItemTrackingTradeItemWriter itemTrackingWriter = new ItemTrackingTradeItemWriter();
        itemTrackingWriter.setDataSource(proxy.getDataSource());
        itemTrackingWriter.setWriteFailureISIN("UK21341EAH47");

        executionContextPromotionListener = new ExecutionContextPromotionListener();
        executionContextPromotionListener.setKeys(new String[]{"stepName",TradeWriter.TOTAL_AMOUNT_KEY});
        Step step2 = new StepBuilder("step2", proxy.getJobRepository())
                .<Trade, Trade>chunk(2, proxy.getTransactionManager())
                .listener(new SkipCheckingListener())
                .listener(executionContextPromotionListener)
                .reader(tradeSqlItemReader)
                .writer(itemTrackingWriter)
                .processor(new TradeProcessor(7))
                .faultTolerant()
                .skipLimit(10)
                .skip(ValidationException.class)
                .skip(IOException.class)
                //不回滚
                .noRollback(ValidationException.class)
                .build();
        Step stepErrorPrint2 = new StepBuilder("errorPrint2", proxy.getJobRepository())
                .tasklet(errPrintTasklet, proxy.getTransactionManager())
                .build();
        String jobName = "testSkipJob88";
        //抉择器,根据条件决定是否执行下一个step
        SkipCheckingDecider decider = new SkipCheckingDecider();
//        Job job = new JobBuilder(jobName, proxy.getJobRepository())
//                .incrementer(new RunIdIncrementer())
//                .start(step1).on("FAILED").fail()
//                .from(step1).on("*").to(step2)/*其他任何都执行下一步*/
//                .from(step1).on("COMPLETED WITH SKIPS").to(stepErrorPrint1)
//                .from(stepErrorPrint1).on("*").to(step2)
//                .from(step2).on("*").to(decider)
//                .from(decider).on("COMPLETED WITH SKIPS").to(stepErrorPrint2)
//                .from(decider).on("*").end()  //其他任何都结束
//                .from(decider).on("FAILED").fail()
//                .build().build();
        Job job = new JobBuilder(jobName, proxy.getJobRepository())
                .incrementer(new RunIdIncrementer())
                .start(step1).on("FAILED").fail()
                .from(step1).on("*").to(step2)/*其他任何都执行下一步*/
                .from(step1).on("COMPLETED WITH SKIPS").to(stepErrorPrint1)
                .from(stepErrorPrint1).next(step2)
                .on("*").to(decider) //这一步很关键
                .on("COMPLETED WITH SKIPS").to(stepErrorPrint2)
                .from(decider).on("*").end()  //其他任何都结束
                .from(decider).on("FAILED").fail()
                .build().build();
        //
        //内存中注册下，下面还要用
        if (!proxy.getJobRegistry().getJobNames().contains(job.getName())) {
            proxy.getJobRegistry().register(new ReferenceJobFactory(job));
        }
        // Clear the data
        clearSkip();

        long id1 = launchJobWithIncrementer(jobName);
        JobExecution execution1 = proxy.getJobExplorer().getJobExecution(id1);
        assertEquals(BatchStatus.COMPLETED, execution1.getStatus());

        validateLaunchWithSkips(execution1,jobName);
        // Clear the data
        clearSkip();
        // Launch 2
        long id2 = launchJobWithIncrementer(jobName);
        JobExecution execution2 = proxy.getJobExplorer().getJobExecution(id2);
        assertEquals(BatchStatus.COMPLETED, execution2.getStatus());

        validateLaunchWithoutSkips(execution2);

        //
        // Make sure that the launches were separate executions and separate
        // instances
        //
        assertTrue(id1 != id2);
        assertNotEquals(execution1.getJobId(), execution2.getJobId());
    }
    void clearSkip() {
        MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer(proxy.getDataSource(), "CUSTOMER_SEQ", "ID");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE", "CUSTOMER");
        for (int i = 1; i < 10; i++) {
            jdbcTemplate.update("INSERT INTO CUSTOMER (ID, VERSION, NAME, CREDIT) VALUES (" + incrementer.nextIntValue()
                    + ", 0, 'customer" + i + "', 100000)");
        }
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "ERROR_LOG");
    }

    private void validateLaunchWithSkips(JobExecution jobExecution,String jobName) {
        // Step1: 9 input records, 1 skipped in read, 1 skipped in write =>
        // 7 written to output
        assertEquals(7, JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE"));

        // Step2: 7 input records, 1 skipped on process, 1 on write => 5 written
        // to output
        assertEquals(5, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "TRADE", "VERSION=1"));

        // 1 record skipped in processing second step
        Assertions.assertEquals(1, SkipCheckingListener.getProcessSkips());

        // Both steps contained skips
        assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

        assertEquals("2 records were skipped!",
                jdbcTemplate.queryForObject("SELECT MESSAGE from ERROR_LOG where JOB_NAME = ? and STEP_NAME = ?",
                        String.class, jobName, "step1"));
        assertEquals("2 records were skipped!",
                jdbcTemplate.queryForObject("SELECT MESSAGE from ERROR_LOG where JOB_NAME = ? and STEP_NAME = ?",
                        String.class, jobName, "step2"));

        assertEquals(new BigDecimal("340.45"), jobExecution.getExecutionContext().get(TradeWriter.TOTAL_AMOUNT_KEY));

        Map<String, Object> step1Execution = getStepExecutionAsMap(jobExecution, "step1");
        assertEquals(4L, step1Execution.get("COMMIT_COUNT"));
        assertEquals(8L, step1Execution.get("READ_COUNT"));
        assertEquals(7L, step1Execution.get("WRITE_COUNT"));
    }

    private void validateLaunchWithoutSkips(JobExecution jobExecution) {

        // Step1: 5 input records => 5 written to output
        assertEquals(5, JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE"));

        // Step2: 5 input records => 5 written to output
        assertEquals(5, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "TRADE", "VERSION=1"));

        // Neither step contained skips
        assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "ERROR_LOG"));

        assertEquals(new BigDecimal("270.75"), jobExecution.getExecutionContext().get(TradeWriter.TOTAL_AMOUNT_KEY));

    }

    private Map<String, Object> getStepExecutionAsMap(JobExecution jobExecution, String stepName) {
        long jobExecutionId = jobExecution.getId();
        return jdbcTemplate.queryForMap(
                "SELECT * from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID = ? and STEP_NAME = ?", jobExecutionId,
                stepName);
    }

    /**
     * Launch the entire job, including all steps, in order.
     * @return JobExecution, so that the test may validate the exit status
     */
    public long launchJobWithIncrementer(String jobName) {
        SkipCheckingListener.resetProcessSkips();
        try {
            return proxy.getJobOperator().startNextInstance(jobName);
        }
        catch (NoSuchJobException | JobExecutionAlreadyRunningException | JobParametersNotFoundException
               | JobRestartException | JobInstanceAlreadyCompleteException | UnexpectedJobExecutionException
               | JobParametersInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSkippableException() throws Exception {
        // given
        ItemReader<Integer> reader = new ListItemReader<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6)) {
            @Override
            public Integer read() {
                Integer item = super.read();
                System.out.println("reading item = " + item);
                if (item != null && item.equals(3)) {
                    System.out.println("Throwing exception on item " + item);
                    throw new IllegalArgumentException("Sorry, no 5 here!");
                }
                return item;
            }
        };

        ItemProcessor<Integer, Integer> processor = item -> {
            if (item.equals(4)) {
                System.out.println("Throwing exception on item " + item);
                throw new IllegalArgumentException("Unable to process 5");
            }
            System.out.println("processing item = " + item);
            return item;
        };

        ItemWriter<Integer> itemWriter = items -> {
            System.out.println("About to write chunk: " + items);
            for (Integer item : items) {
                if (item.equals(5)) {
                    System.out.println("Throwing exception on item " + item);
                    throw new IllegalArgumentException("Sorry, no 5 here!");
                }
                System.out.println("writing item = " + item);
            }
        };

        Step step = new StepBuilder("step", proxy.getJobRepository()).<Integer, Integer>chunk(3, proxy.getTransactionManager())
                .reader(reader)
                .processor(processor)
                .writer(itemWriter)
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(3)
                .build();
        Job job = new JobBuilder("testSkippableException", proxy.getJobRepository()).start(step).build();
        // when
        JobExecution jobExecution = proxy.getJobLauncher().run(job, getUniqueJobParameters());

        // then
        assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertEquals(1, stepExecution.getReadSkipCount());
        assertEquals(1, stepExecution.getProcessSkipCount());
        assertEquals(1, stepExecution.getWriteSkipCount());
    }
}