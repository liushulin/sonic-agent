package org.cloud.sonic.agent.tests.handlers;

import cn.hutool.http.HttpUtil;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.ios.options.wda.XcodeCertificate;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.cloud.sonic.agent.common.models.HandleContext;
import org.cloud.sonic.driver.common.models.BaseResp;
import org.cloud.sonic.driver.ios.IOSDriver;
import org.openqa.selenium.By;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SinovaIOSStepHandler {

    private static io.appium.java_client.ios.IOSDriver appiumIOSDriver;

    public static void toWebView(HandleContext handleContext, IOSDriver iosDriver, boolean retry) {
//        packageName = TextHandler.replaceTrans(packageName, globalParams);
//        process = TextHandler.replaceTrans(process, globalParams);
//        handleContext.setStepDes("切换到" + packageName + " WebView");
//        handleContext.setDetail("AndroidProcess: " + process);
        try {

            if(appiumIOSDriver != null){
                appiumIOSDriver.quit();
                appiumIOSDriver = null;
            }


            XCUITestOptions options = new XCUITestOptions();
            options.setPlatformName("iOS");
            options.setPlatformVersion("18.4.1");
            options.setDeviceName("刘澍霖iPhone11");
//            options.setBundleId("com.chinaunicom.mobilebusiness");
            options.setUdid("00008030-001650D022B8802E");
            options.setUseNewWDA(false);
            options.setNoReset(true);
            options.setAutomationName("XCUITest");
            options.setXcodeCertificate(new XcodeCertificate("juejiezhang","Apple Developer"));
            List<String> bundleIds = new ArrayList<>();
            bundleIds.add("com.chinaunicom.mobilebusiness");
            options.setAdditionalWebviewBundleIds(bundleIds);

            System.out.println("当前WDA RemoteURL："+iosDriver.getWdaClient().getRemoteUrl());

            appiumIOSDriver = new io.appium.java_client.ios.IOSDriver(new URL("http://127.0.0.1:4723"), options);

            Set<String> handlers = appiumIOSDriver.getContextHandles();
            for (String handler : handlers) {
                System.out.println(handler);
            }
            handleContext.setStepDes(appiumIOSDriver.getSessionId()+"");
            handleContext.setDetail(iosDriver.getWdaClient().getSessionId());

            appiumIOSDriver.context(handlers.toArray()[handlers.size()-1].toString());
//            System.out.println(appiumIOSDriver.getPageSource());

            appiumIOSDriver.findElement(By.cssSelector("#broadImg > div > div.van-tabs__content > div:nth-child(2) > div > div.secondPart.style-module_phoneSecond_1EiW9 > div.van-tabs.van-tabs--line > div.van-tabs__content > div > div.style-module_payButtonWrap_3wHqR > div.style-module_payBtn_3HMit")).click();




        } catch (Exception e) {
            handleContext.setE(e);
        }
    }

}
