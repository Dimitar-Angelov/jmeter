/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.apache.jmeter.protocol.java.sampler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestListener;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * @author pete
 *
 * This is a basic implementation that runs a single test method of
 * a JUnit test case. The current implementation doesn't support
 * oneTimeSetUp yet. Still need to think about that more thoroughly
 * and decide how it should be called.
 */
public class JUnitSampler extends AbstractSampler implements TestListener {

    /**
     * Property key representing the classname of the JavaSamplerClient to
     * user.
     */
    public static final String CLASSNAME = "junitSampler.classname";
    public static final String METHOD = "junitsampler.method";
    public static final String FAILURE = "junitsampler.failure";
    public static final String FAILURECODE = "junitsampler.failure.code";
    public static final String SUCCESS = "junitsampler.success";
    public static final String SUCCESSCODE = "junitsampler.success.code";
    public static final String FILTER = "junitsampler.pkg.filter";
    public static final String DOSETUP = "junitsampler.exec.setup";
    
    public static final String SETUP = "setUp";
    public static final String TEARDOWN = "tearDown";
    public static final String ONETIMESETUP = "oneTimeSetUp";
    public static final String ONETIMETEARDOWN = "oneTimeTearDown";
    /// the Method objects for setUp and tearDown methods
    protected Method SETUP_METHOD = null;
    protected Method TDOWN_METHOD = null;
    protected Method ONETIME_SETUP_METHOD = null;
    protected Method ONETIME_TDOWN_METHOD = null;
    protected boolean checkStartUpTearDown = false;
    
    protected TestCase TEST_INSTANCE = null;
    
    /**
     * Logging
     */
    private static transient Logger log = LoggingManager.getLoggerForClass();

    public JUnitSampler(){
    }
    
    /**
     * Method tries to get the setUp and tearDown method for the class
     * @param tc
     */
    public void initMethodObjects(TestCase tc){
        if (!this.checkStartUpTearDown){
            if (ONETIME_SETUP_METHOD == null){
                ONETIME_SETUP_METHOD = getMethod(tc,ONETIMESETUP);
            }
            if (ONETIME_TDOWN_METHOD == null){
                ONETIME_TDOWN_METHOD = getMethod(tc,ONETIMETEARDOWN);
            }
            if (!getDoNotSetUpTearDown()) {
                if (SETUP_METHOD == null){
                    SETUP_METHOD = getMethod(tc,SETUP);
                }
                if (TDOWN_METHOD == null){
                    TDOWN_METHOD = getMethod(tc,TEARDOWN);
                }
            }
            this.checkStartUpTearDown = true;
        }
    }
    
    /**
     * Sets the Classname attribute of the JavaConfig object
     *
     * @param  classname  the new Classname value
     */
    public void setClassname(String classname)
    {
        setProperty(CLASSNAME, classname);
    }

    /**
     * Gets the Classname attribute of the JavaConfig object
     *
     * @return  the Classname value
     */
    public String getClassname()
    {
        return getPropertyAsString(CLASSNAME);
    }
    
    /**
     * Return the name of the method to test
     * @return
     */
    public String getMethod(){
        return getPropertyAsString(METHOD);
    }

    /**
     * Method should add the JUnit testXXX method to the list at
     * the end, since the sequence matters.
     * @param methodName
     */
    public void setMethod(String methodName){
        setProperty(METHOD,methodName);
    }
    
    /**
     * get the success message
     * @return
     */
    public String getSuccess(){
        return getPropertyAsString(SUCCESS);
    }
    
    /**
     * set the success message
     * @param success
     */
    public void setSuccess(String success){
        setProperty(SUCCESS,success);
    }
    
    /**
     * get the success code defined by the user
     * @return
     */
    public String getSuccessCode(){
        return getPropertyAsString(SUCCESSCODE);
    }

    /**
     * set the succes code. the success code should
     * be unique.
     * @param code
     */
    public void setSuccessCode(String code){
        setProperty(SUCCESSCODE,code);
    }
    
    /**
     * get the failure message
     * @return
     */
    public String getFailure(){
        return getPropertyAsString(FAILURE);
    }

    /**
     * set the failure message
     * @param fail
     */
    public void setFailure(String fail){
        setProperty(FAILURE,fail);
    }
    
    /**
     * The failure code is used by other components
     * @return
     */
    public String getFailureCode(){
        return getPropertyAsString(FAILURECODE);
    }
    
    /**
     * Provide some unique code to denote a type of failure
     * @param code
     */
    public void setFailureCode(String code){
        setProperty(FAILURECODE,code);
    }

    /**
     * return the comma separated string for the filter
     * @return
     */
    public String getFilterString(){
        return getPropertyAsString(FILTER);
    }
    
    /**
     * set the filter string in comman separated format
     * @param text
     */
    public void setFilterString(String text){
        setProperty(FILTER,text);
    }
    
    public boolean getDoNotSetUpTearDown(){
        return getPropertyAsBoolean(DOSETUP);
    }
    
    public void setDoNotSetUpTearDown(boolean setup){
        setProperty(DOSETUP,String.valueOf(setup));
    }
    
    /* (non-Javadoc)
	 * @see org.apache.jmeter.samplers.Sampler#sample(org.apache.jmeter.samplers.Entry)
	 */
	public SampleResult sample(Entry entry) {
		SampleResult sresult = new SampleResult();
        sresult.setSampleLabel(JUnitSampler.class.getName());
        sresult.setSamplerData(getClassname() + "." + getMethod());
        this.testStarted("");
        if (this.TEST_INSTANCE != null){
            initMethodObjects(this.TEST_INSTANCE);
            // create a new TestResult
            TestResult tr = new TestResult();
            try {
                if (!getDoNotSetUpTearDown() && SETUP_METHOD != null){
                    SETUP_METHOD.invoke(TEST_INSTANCE,new Class[0]);
                    log.info("called setUp");
                }
                Method m = getMethod(TEST_INSTANCE,getMethod());
                sresult.sampleStart();
                m.invoke(TEST_INSTANCE,null);
                sresult.sampleEnd();
                // log.info("invoked " + getMethod());
                if (!getDoNotSetUpTearDown() && TDOWN_METHOD != null){
                    TDOWN_METHOD.invoke(TEST_INSTANCE,new Class[0]);
                    log.info("called tearDown");
                }
            } catch (InvocationTargetException e) {
                log.warn(e.getMessage());
            } catch (IllegalAccessException e) {
                log.warn(e.getMessage());
            }
            if ( !tr.wasSuccessful() ){
                sresult.setSuccessful(false);
                StringBuffer buf = new StringBuffer();
                buf.append(getFailure());
                Enumeration en = tr.errors();
                while (en.hasMoreElements()){
                    buf.append((String)en.nextElement());
                }
                sresult.setResponseMessage(buf.toString());
                sresult.setResponseCode(getFailureCode());
            } else {
                // this means there's no failures
                sresult.setSuccessful(true);
                sresult.setResponseMessage(getSuccess());
                sresult.setResponseCode(getSuccessCode());
            }
        } else {
            // we should log a warning, but allow the test to keep running
            sresult.setSuccessful(false);
            // this should be externalized to the properties
            sresult.setResponseMessage("failed to create an instance of the class");
        }
		return sresult;
	}

    /**
     * If the method is not able to create a new instance of the
     * class, it returns null and logs all the exceptions at
     * warning level.
     * @return
     */
    public Object getClassInstance(){
        if (getClassname() != null){
            try {
                Class clazz = Class.forName(getClassname());
                return clazz.getDeclaredConstructor(new Class[0]).newInstance(null);
            } catch (ClassNotFoundException ex){
                log.warn(ex.getMessage());
            } catch (IllegalAccessException ex){
                log.warn(ex.getMessage());
            } catch (NoSuchMethodException ex){
                log.warn(ex.getMessage());
            } catch (InvocationTargetException ex){
                log.warn(ex.getMessage());
            } catch (InstantiationException ex){
                log.warn(ex.getMessage());
            }
        }
        return null;
    }
    
    /**
     * 
     * @param clazz
     * @param method
     * @return
     */
    public Method getMethod(Object clazz, String method){
        if (clazz != null && method != null){
            log.info("class " + clazz.getClass().getName() +
                    " method name is " + method);
            try {
                return clazz.getClass().getMethod(method,new Class[0]);
            } catch (NoSuchMethodException e) {
                log.warn(e.getMessage());
            }
        }
        return null;
    }
    
    public void testStarted(){
    }

    /**
     * method will call oneTimeTearDown to clean things up. It is only called
     * at the end of the test.
     */
    public void testEnded() {
        if (this.TEST_INSTANCE == null) {
            this.TEST_INSTANCE = (TestCase)getClassInstance();
            initMethodObjects(this.TEST_INSTANCE);
            if (ONETIME_TDOWN_METHOD != null && this.TEST_INSTANCE != null) {
                try {
                    ONETIME_TDOWN_METHOD.invoke(this.TEST_INSTANCE,new Class[0]);
                    log.info("oneTimeTearDown invoked");
                } catch (IllegalAccessException ex){
                    log.warn(ex.getMessage());
                } catch (InvocationTargetException ex){
                    log.warn(ex.getMessage());
                } catch (IllegalArgumentException ex) {
                    log.warn(ex.getMessage());
                }
            } else {
                if (this.TEST_INSTANCE == null) {
                    log.info("testEnded - oneTimeTearDown and test class were null");
                } else {
                    log.info("testEnded - oneTimeTearDown was null");
                }
            }
        }
    }

    /**
     * method will call oneTimeSetUp to setup the unit test
     */
    public void testStarted(String host) {
        if (this.TEST_INSTANCE == null) {
            this.TEST_INSTANCE = (TestCase)getClassInstance();
            initMethodObjects(this.TEST_INSTANCE);
            if (ONETIME_SETUP_METHOD != null && this.TEST_INSTANCE != null) {
                try {
                    ONETIME_SETUP_METHOD.invoke(this.TEST_INSTANCE,new Class[0]);
                    log.info("oneTimeSetUp invoked");
                } catch (IllegalAccessException ex){
                    log.warn(ex.getMessage());
                } catch (InvocationTargetException ex){
                    log.warn(ex.getMessage());
                } catch (IllegalArgumentException ex) {
                    log.warn(ex.getMessage());
                }
            } else {
                if (this.TEST_INSTANCE == null) {
                    log.info("testStarted - oneTimeSetUp and test class were null");
                } else {
                    log.info("testStarted - oneTimeSetUp was null");
                }
            }
        }
    }

    public void testEnded(String host) {
        
    }

    /**
     * Method is not implemented, but required by TestListener
     */
    public void testIterationStart(LoopIterationEvent event) {
        
    }
}
