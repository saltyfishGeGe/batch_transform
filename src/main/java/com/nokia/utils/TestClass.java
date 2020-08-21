package com.nokia.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClass {

    Logger LOG = LoggerFactory.getLogger(TestClass.class);

    public void aa(){
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.info("aaaaa");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.error("eeeeee");
        LOG.warn("wwwwwww");
        LOG.warn("wwwwwww");
        LOG.warn("wwwwwww");
        LOG.warn("wwwwwww");
        LOG.warn("wwwwwww");
        LOG.warn("wwwwwww");
        LOG.warn("wwwwwww");
        LOG.warn("wwwwwww");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
        LOG.debug("dddddd");
    }

    public static void main(String[] args) {
        TestClass t = new TestClass();
        t.aa();

    }
}
