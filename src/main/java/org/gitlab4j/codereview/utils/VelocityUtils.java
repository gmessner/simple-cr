package org.gitlab4j.codereview.utils;

import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class VelocityUtils {

    /**
     * Merges the provided parameters with a Velocity template, returning the results as a String.
     * 
     * @param template
     * @param params
     * @return the expanded Velocity template as a String
     * @throws Exception
     */
    public static String getTextBody(String template, Map<String, Object> params) throws Exception {

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("resource.loader", "class");
        ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ve.init();

        StringWriter sw = new StringWriter();
        VelocityContext context = new VelocityContext(params);

        Template tplt = ve.getTemplate(template);
        tplt.merge(context, sw);

        return sw.toString();
    }

    /**
     * Reads a Velocity template, merges the parameters, returning the results as a String.
     * 
     * @param reader
     * @param params
     * @return the expanded Velocity template as a String
     * @throws Exception
     */
    public static String getTextBody(Reader reader, Map<String, Object> params) throws Exception {

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("resource.loader", "class");
        ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ve.init();

        StringWriter sw = new StringWriter();
        VelocityContext context = new VelocityContext(params);
        ve.evaluate(context, sw, "TemplateService", reader);

        return sw.toString();
    }
}
