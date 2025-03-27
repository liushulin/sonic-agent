package org.cloud.sonic.agent.tests.handlers;

import com.alibaba.fastjson2.JSONObject;
import org.cloud.sonic.agent.common.models.HandleContext;
import org.cloud.sonic.agent.tests.LogUtil;
import org.cloud.sonic.driver.common.tool.SonicRespException;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 炎黄扩展类
 *
 */
public class SinovaAndroidStepHandler {

    private static final String FGF = "||";
    private static final String FGFRegex = "\\|\\|";

    private static final String StepTitle = "执行自定义扩展任务";

    /**
     * value格式：#wddd > div.tabsCust > div > div:nth-child(1) > div > div > div > div:nth-child(1) > span
     * 或者：#wddd > div.tabsCust > div > div:nth-child(1) > div > div > div > div:nth-child(1) > span || {"type":"类型扩展","x":"1","y":"2"}
     * @param pathValue
     * @return
     */
    public static String removeExtendedCharacterInfo(String pathValue){
        if (pathValue.contains(FGF)){
            //按照自定义规范格式加载
            pathValue = pathValue.split(FGFRegex)[0].trim();
        }
        return pathValue;
    }


    /**
     * 针对不同的分辨率转换坐标
     * @param androidStepHandler
     * @param baseX 录制时的基准坐标
     * @param baseY 录制时的基准坐标
     * @param baseWidth 录制时的设备的逻辑像素宽度
     * @param baseHeight 录制时的设备的逻辑像素高度
     * @return
     * @throws Exception
     */
    public static int[] convertCoordinatesByResolution(AndroidStepHandler androidStepHandler,int baseX,int baseY,int baseWidth,int baseHeight) throws Exception{

        //计算当前设备的逻辑像素宽度和高度
        double currentWidth = (double) androidStepHandler.screenWidth / ((double) androidStepHandler.density / 160);
        double currentHeight = (double) androidStepHandler.screenHeight / ((double) androidStepHandler.density / 160);

        int newX = (int) (baseX * currentWidth / baseWidth);
        int newY = (int) (baseY * currentHeight / baseHeight);

        return new int[]{newX,newY};
    }



    /**
     * 自定义 webview click 方法
     * @param androidStepHandler
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     */
    public static void webviewClick(AndroidStepHandler androidStepHandler,ChromeDriver chromeDriver,HandleContext handleContext, String des, String selector, String pathValue) throws Exception{
        if(pathValue.contains(FGF)){
            String p = (pathValue.split(FGFRegex))[0].trim();
            String extra = pathValue.split(FGFRegex)[1].trim();

            JSONObject jo = JSONObject.parseObject(extra);
            String type = jo.getString("type");
            if("css::coord".equals(type)){
                //解析采集设备的基准逻辑坐标
                int x = jo.getIntValue("x",0);
                int y = jo.getIntValue("y",0);
                //解析采集设备的基准逻辑宽高
                int baseWidth = jo.getIntValue("w",0);
                int baseHeight = jo.getIntValue("h",0);
                //针对不同分辨率转换坐标
                int[] newXY = convertCoordinatesByResolution(androidStepHandler,x,y,baseWidth,baseHeight);
                WebElement element = androidStepHandler.findWebEle(selector, pathValue);
                int target_X = element.getLocation().getX() + newXY[0];
                int target_Y = element.getLocation().getY() + newXY[1];

                handleContext.setStepDes(StepTitle);
                handleContext.setDetail("处理::after类型的元素，首先找到它的父元素【"+pathValue+"】，处理不同分辨率的坐标转换：相对于父元素的坐标（"+newXY[0]+","+newXY[1]+"），点击相对于屏幕原点的坐标（"+target_X+","+target_Y+"）");

                chromeDriver.executeScript("document.elementFromPoint(arguments[0],arguments[1]).click()",target_X,target_Y);
            }else{
                //按原来的规范点击
                androidStepHandler.findWebEle(selector,pathValue).click();
            }


        }else{
            //按原来的规范点击
            androidStepHandler.findWebEle(selector,pathValue).click();
        }
    }


    /**
     * 自定义 webview moveToElement 方法
     * @param androidStepHandler
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     */
    public static void webElementScrollToView(AndroidStepHandler androidStepHandler,ChromeDriver chromeDriver,HandleContext handleContext, String des, String selector, String pathValue) {
        //是否包含自定义扩展
        if(pathValue.contains(FGF)){
            handleContext.setStepDes("滚动页面元素 " + des + " 至可见，因元素在swiper中，循环滚动swiper每一页直至元素可见");
            WebElement we;
            try {
                we = androidStepHandler.findWebEle(selector, pathValue);
            } catch (Exception e) {
                handleContext.setE(e);
                return;
            }

            String p = (pathValue.split(FGFRegex))[0].trim();
            String extra = pathValue.split(FGFRegex)[1].trim();

            JSONObject jo = JSONObject.parseObject(extra);
            String swiperTag = jo.getString("swiper");
            if(swiperTag!=null){
                WebElement swiperElement = null;
                try {
                    swiperElement = androidStepHandler.findWebEle(selector, swiperTag);
                } catch (Exception e) {
                    handleContext.setE(e);
                    return;
                }

                //滑动到元素可见
//                WebDriverWait wait = new WebDriverWait(chromeDriver, Duration.ofSeconds(1));
//                WebElement targetElement = ExpectedConditions.visibilityOfElementLocated(By.cssSelector(p)).apply(chromeDriver);
                int count = 0;
                //最多循环的次数
                while (!we.isDisplayed() && count < 10){
                    count++;
                    //滑动swiper到下一页
                    chromeDriver.executeScript("arguments[0].swiper.slideNext();", swiperElement);
                }
            }

        }else{
            //按原来的逻辑
            handleContext.setStepDes("滚动页面元素 " + des + " 至顶部可见");
            WebElement we;
            try {
                we = androidStepHandler.findWebEle(selector, pathValue);
            } catch (Exception e) {
                handleContext.setE(e);
                return;
            }

            //效果：X轴和Y轴都滚动到屏幕的顶部
//            chromeDriver.executeScript("arguments[0].scrollIntoView();", we);


//            behavior：指定滚动的行为，可取的值有 'auto'（瞬间滚动）和 'smooth'（平滑滚动）。
//            block：指定元素在垂直方向上的对齐方式，可取的值有 'start'（顶部对齐）、'center'（居中对齐）、'end'（底部对齐）和 'nearest'（最近对齐）。
//            inline：指定元素在水平方向上的对齐方式，可取的值有 'start'、'center'、'end' 和 'nearest'。

            //只滚动X轴或者Y轴，保持在相应方向上底部对齐，另外一个方向保持不变
            Long webWidnowWidth = (Long) chromeDriver.executeScript("return window.innerWidth;");
            Long webWindowHeight = (Long) chromeDriver.executeScript("return window.innerHeight;");

            Double top = (Double) chromeDriver.executeScript("return arguments[0].getBoundingClientRect().top;", we);
            Double left = (Double) chromeDriver.executeScript("return arguments[0].getBoundingClientRect().left;", we);

            //处理X轴
            if(left > webWidnowWidth){
                //元素在右侧
                chromeDriver.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block:'nearest', inline: 'end'});", we);
            }else if(left < 0){
                //元素在左侧
                chromeDriver.executeScript("arguments[0].scrollIntoView({behavior: 'smooth',  block:'nearest',  inline: 'start'});", we);
            }

            //处理Y轴
            if(top > webWindowHeight){
                //元素在右侧
                chromeDriver.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block:'end', inline: 'nearest'});", we);
            }else if(top < 0){
                //元素在底部
                chromeDriver.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block:'start', inline: 'nearest'});", we);
            }

            handleContext.setDetail("控件元素 " + selector + ":" + pathValue + " 滚动至页面顶部");
        }

    }






}



