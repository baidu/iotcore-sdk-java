package com.baidu.iot.device.sdk.avatar.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * @Author zhangxiao18
 * @Date 2020/10/18
 */
public class PropertyValueTest {

    @Test
    public void testCreate() {
        String strValue = "\"cccc\"";
        PropertyValue propertyValue = new PropertyValue(strValue);
        Assert.assertEquals(strValue, propertyValue.toString());

        String intValue = "1234";
        PropertyValue propertyValue1 = new PropertyValue(intValue);
        Assert.assertEquals(intValue, propertyValue1.toString());

        String objValue = "{\"a\":\"bbb\"}";
        PropertyValue propertyValue2 = new PropertyValue(objValue);
        Assert.assertEquals(objValue, propertyValue2.toString());
    }

    @Test
    public void testFindChild() {
        PropertyValue parent = new PropertyValue("{"
                + "        \"a\": 3,"
                + "        \"b\": {"
                + "            \"c\": 5"
                + "        }"
                + "    }");
        Assert.assertEquals(parent.toString(), parent.getValue(new PropertyKey("")).toString());

        Assert.assertEquals(String.valueOf(3), parent.getValue(new PropertyKey("a")).toString());
        Assert.assertEquals(String.valueOf("{\"c\":5}"), parent.getValue(new PropertyKey("b")).toString());
        Assert.assertEquals(String.valueOf(5), parent.getValue(new PropertyKey("b.c")).toString());

        Assert.assertEquals("null", parent.getValue(new PropertyKey("a.d")).toString());
        Assert.assertEquals("null", parent.getValue(new PropertyKey("d")).toString());

    }

}
