package com.alibaba.datax.plugin.writer.oceanbasev10writer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.transport.record.DefaultRecord;
import com.alibaba.datax.core.util.ConfigParser;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;
import com.alibaba.datax.plugin.rdbms.writer.Constant;
import com.alibaba.datax.plugin.writer.oceanbasev10writer.OceanBaseWriterTask;
import com.alibaba.datax.plugin.writer.oceanbasev10writer.buffer.RuleWriterDbBuffer;
import com.alibaba.datax.plugin.writer.oceanbasev10writer.groovy.GroovyRuleExecutor;

/**
 * Date: 2015/5/19 16:33
 *
 * @author Administrator <a href="mailto:liupengjava@gmail.com">Ricoul</a>
 */
public class OceanBaseWriterTaskTest {

    @Test
    public void testTaskInit() throws Exception {
    	OceanBaseWriterTask task = new OceanBaseWriterTask(DataBaseType.MySql);
        Configuration jobConf = ConfigParser.parseJobConfig(Thread.currentThread().getClass().getResource("/").getPath() + File.separator + "basic1.json");
        jobConf = jobConf.getConfiguration(CoreConstant.DATAX_JOB_CONTENT_WRITER_PARAMETER);
        jobConf.set(Constant.INSERT_OR_REPLACE_TEMPLATE_MARK, "");
        task.init(jobConf);

        assertEquals((String) ReflectionTestUtils.getField(task, "dbNamePattern"), "datax_3_mysqlrulewriter_{00}");
        assertEquals((String) ReflectionTestUtils.getField(task, "dbRule"), "((#i1#).longValue() % 8).intdiv(4)");
        assertEquals((String) ReflectionTestUtils.getField(task, "tableNamePattern"), "mysql_writer_test_case00_{0}");
        assertEquals((String) ReflectionTestUtils.getField(task, "tableRule"), "((#i1#).longValue() % 8)");
    }

    @Test
    public void testConvertRecord2Map() {
    	OceanBaseWriterTask task = new OceanBaseWriterTask(DataBaseType.MySql);

        Record r = new DefaultRecord();
        r.addColumn(new StringColumn("aaa"));

        List<String> columnList = new ArrayList<String>();
        columnList.add("UPPER_CASE");

        Triple<List<String>, List<Integer>, List<String>> resultSetMetaData = new ImmutableTriple<List<String>, List<Integer>, List<String>>(
                columnList, new ArrayList<Integer>(),
                new ArrayList<String>());


        ReflectionTestUtils.setField(task, "resultSetMetaData", resultSetMetaData);
        ReflectionTestUtils.setField(task, "columnNumber", 1);
        Map<String,Object> map = task.convertRecord2Map(r);

        assertEquals(map.get("upper_case"), "aaa");
    }

    @Test
    public void testGetDbNameFromJdbcUrl() {
    	OceanBaseWriterTask task = new OceanBaseWriterTask(DataBaseType.MySql);
        String jdbcUrl = "jdbc:mysql://10.232.130.106:3306/datax_3_mysqlwriter?yearIsDateType=false&zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=true&tinyInt1isBit=false";
        assertEquals("datax_3_mysqlwriter", task.getDbNameFromJdbcUrl(jdbcUrl));

        jdbcUrl = "jdbc:mysql://10.232.130.106:3306/datax_3_mysqlwriter";
        assertEquals("datax_3_mysqlwriter", task.getDbNameFromJdbcUrl(jdbcUrl));
    }

    @Test
    public void testCheckRule多库多表每一张表都不相同() {
    	OceanBaseWriterTask task = new OceanBaseWriterTask(DataBaseType.MySql);

        //多库多表，并且每一张表都不相同，tableRule为必填
        List<RuleWriterDbBuffer> dbBufferList = new ArrayList<RuleWriterDbBuffer>();
        dbBufferList.add(generateNewBuffer("db01:tb01,tb02"));
        dbBufferList.add(generateNewBuffer("db02:tb03,tb04"));
        try {
            task.checkRule(dbBufferList);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("但未配置分表规则"));
        }

        ReflectionTestUtils.setField(task, "tableRuleExecutor", new GroovyRuleExecutor("", ""));
        task.checkRule(dbBufferList);

        //多库多表，并且每一张表都不相同，tableRule为必填
        dbBufferList = new ArrayList<RuleWriterDbBuffer>();
        dbBufferList.add(generateNewBuffer("db01:tb01,tb02"));
        dbBufferList.add(generateNewBuffer("db01:tb03,tb04"));
        task.checkRule(dbBufferList);
    }

    @Test
    public void testCheckRule多库多表表名部分表相同() {
    	OceanBaseWriterTask task = new OceanBaseWriterTask(DataBaseType.MySql);
        ReflectionTestUtils.setField(task, "tableRuleExecutor", new GroovyRuleExecutor("", ""));
        List<RuleWriterDbBuffer> dbBufferList = new ArrayList<RuleWriterDbBuffer>();
        dbBufferList.add(generateNewBuffer("db01:tb01,tb02"));
        dbBufferList.add(generateNewBuffer("db02:tb01,tb02"));
        try {
            task.checkRule(dbBufferList);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("但未配置分库规则和分表规则"));
        }

        ReflectionTestUtils.setField(task, "dbRuleExecutor", new GroovyRuleExecutor("", ""));
        task.checkRule(dbBufferList);
    }

    @Test
    public void testCheckRule多库多表库名全部不同表名全部相同() {
    	OceanBaseWriterTask task = new OceanBaseWriterTask(DataBaseType.MySql);
        List<RuleWriterDbBuffer> dbBufferList = new ArrayList<RuleWriterDbBuffer>();
        dbBufferList.add(generateNewBuffer("db01:tb01"));
        dbBufferList.add(generateNewBuffer("db02:tb01"));
        try {
            task.checkRule(dbBufferList);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("但未配置分库规则"));
        }

        ReflectionTestUtils.setField(task, "dbRuleExecutor", new GroovyRuleExecutor("", ""));
        task.checkRule(dbBufferList);
    }

    @Test
    public void testCheckRule多库多表库名表名全部相同() {
    	OceanBaseWriterTask task = new OceanBaseWriterTask(DataBaseType.MySql);
        List<RuleWriterDbBuffer> dbBufferList = new ArrayList<RuleWriterDbBuffer>();
        dbBufferList.add(generateNewBuffer("db01:tb01"));
        dbBufferList.add(generateNewBuffer("db01:tb01"));
        try {
            task.checkRule(dbBufferList);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("此种回流方式不支持"));
        }
    }

    public RuleWriterDbBuffer generateNewBuffer(String dbTableStr) {
        String dbName = dbTableStr.split(":")[0];
        String[] tableArray = dbTableStr.split(":")[1].split(",");
        RuleWriterDbBuffer ruleWriterDbBuffer = new RuleWriterDbBuffer();
        ruleWriterDbBuffer.setDbName(dbName);
        List<String> tableList = Arrays.asList(tableArray);
        ruleWriterDbBuffer.initTableBuffer(tableList);
        return ruleWriterDbBuffer;
    }

}
