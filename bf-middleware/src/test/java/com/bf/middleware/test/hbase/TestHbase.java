package com.bf.middleware.test.hbase;

import org.apache.hadoop.hbase.util.Bytes;
import org.bf.framework.autoconfigure.hbase.HbaseProxy;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;

public class TestHbase  implements BaseCoreTest {
    private String tableName = "test:log";

    public static byte[] CF_INFO = Bytes.toBytes("f");

    private byte[] qUser = Bytes.toBytes("user");
    private byte[] qEmail = Bytes.toBytes("email");
    private byte[] qPassword = Bytes.toBytes("password");
    @Autowired
    @Qualifier("bf.hadoop.hz_HbaseProxy")
    private HbaseProxy hbaseProxy;

    @Test
    public void testHz() throws IOException {
//        initialize();

//        User u = hzTemplate.execute(tableName, table -> {
//            User user = new User("tom", "123@qq.com", "pwd");
//            Put p = new Put(Bytes.toBytes(user.getName()));
//            p.addColumn(CF_INFO, qUser, Bytes.toBytes(user.getName()));
//            p.addColumn(CF_INFO, qEmail, Bytes.toBytes(user.getEmail()));
//            p.addColumn(CF_INFO, qPassword, Bytes.toBytes(user.getPassword()));
//            table.put(p);
//            return user;
//
//        });

    }


    @Test
    public void testFind() throws IOException {
//        List<User> userList = hzTemplate.find(tableName, "f", new RowMapper<User>() {
//            @Override
//            public User mapRow(Result result, int rowNum) throws Exception {
//                return new User(Bytes.toString(result.getValue(CF_INFO, qUser)),
//                        Bytes.toString(result.getValue(CF_INFO, qEmail)),
//                        Bytes.toString(result.getValue(CF_INFO, qPassword)));
//            }
//        });
//        System.out.println(userList);
    }

}
