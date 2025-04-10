package org.cloud.sonic.agent.tests.handlers;

import com.alibaba.fastjson2.JSONObject;
import org.cloud.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import org.cloud.sonic.agent.common.interfaces.StepType;
import org.cloud.sonic.agent.common.models.HandleContext;
import org.cloud.sonic.agent.tests.LogUtil;
import org.cloud.sonic.agent.tools.BytesTool;
import org.cloud.sonic.driver.android.service.AndroidElement;
import org.cloud.sonic.driver.common.models.BaseElement;
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

    /**
     * 删除控件元素pathvalue中的自定义扩展的
     * value格式：#wddd > div.tabsCust > div > div:nth-child(1) > div || [x=340,y=22,w=360,h=748]
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
     * 扩展自定义规范格式：
     * （1）value格式：#wddd > div.tabsCust > div > div:nth-child(1) > div || [x=340,y=22,w=360,h=748]
     * @param androidStepHandler
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     */
    public static void webviewClick(AndroidStepHandler androidStepHandler,ChromeDriver chromeDriver,HandleContext handleContext, String des, String selector, String pathValue) throws Exception{

        if(pathValue.contains(FGF)){
            String p = (pathValue.split(FGFRegex))[0].trim();
            String extra = (pathValue.split(FGFRegex))[1].trim();

            //符合规范（1）：点击父元素控件内的某一个坐标，w和h是获取控件时手机的逻辑像素宽度和高度，x和y是相对于父控件的逻辑像素坐标
            if(extra.contains("x=") && extra.contains("y=") && extra.contains("w=") && extra.contains("h=")){
                //解析坐标字符串 [x=340,y=22,w=360,h=748]
                String coordStr = extra.replaceAll("[\\[\\]]", "");
                String[] coords = coordStr.split(",");
                int x = 0, y = 0, w = 0, h = 0;
                for (String coord : coords) {
                    String[] kv = coord.split("=");
                    if (kv.length == 2) {
                        switch (kv[0].trim()) {
                            case "x": x = Integer.parseInt(kv[1].trim()); break;
                            case "y": y = Integer.parseInt(kv[1].trim()); break;
                            case "w": w = Integer.parseInt(kv[1].trim()); break;
                            case "h": h = Integer.parseInt(kv[1].trim()); break;
                        }
                    }
                }

                //针对不同分辨率转换坐标
                int[] newXY = convertCoordinatesByResolution(androidStepHandler,x,y,w,h);
                WebElement element = androidStepHandler.findWebEle(selector, p);
                int target_X = element.getLocation().getX() + newXY[0];
                int target_Y = element.getLocation().getY() + newXY[1];

                handleContext.setStepDes("点击自定义语法规范的控件 "+pathValue);

                handleContext.setDetail("首先找到它的父元素【"+p+"】，处理不同分辨率的坐标转换：相对于父元素的坐标（"+newXY[0]+","+newXY[1]+"），点击相对于屏幕原点的坐标（"+target_X+","+target_Y+"）");

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
     * 自定义 webview scroll 方法，滚动到元素可见
     * 支持 swiper 组件，如果是swiper，使用swiper自己的方法滚动到下一页
     * @param androidStepHandler
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     */
    public static void webElementScrollToView(AndroidStepHandler androidStepHandler,ChromeDriver chromeDriver,HandleContext handleContext, String des, String selector, String pathValue) {
        //检查父元素是否是swiper
        try {
            WebElement we = androidStepHandler.findWebEle(selector, pathValue);

            //先查找父元素，判断是否是swiper
            WebElement pwe = null;
            try {
                pwe = we.findElement(By.xpath("//parent::*[contains(@class,'swiper-containeres')]"));
            } catch (Exception e) {
                //.....
            }

            if(pwe!=null){
                //是swiper
                handleContext.setStepDes("滚动页面元素 " + des + " 至顶部可见");
                handleContext.setDetail("因元素在swiper中，循环滚动swiper每一页直至元素可见");
                int count = 0;
                //最多循环的次数
                while (!we.isDisplayed() && count < 100){
                    count++;
                    //滑动swiper到下一页
                    chromeDriver.executeScript("arguments[0].swiper.slideNext();", pwe);
                }
                return;

            }else {
                //不是swiper，按普通的scroll逻辑执行
                handleContext.setStepDes("滚动页面元素 " + des + " 至顶部可见");
                try{
                    /*
                        behavior：指定滚动的行为，可取的值有 'auto'（瞬间滚动）和 'smooth'（平滑滚动）。
                        block：指定元素在垂直方向上的对齐方式，可取的值有 'start'（顶部对齐）、'center'（居中对齐）、'end'（底部对齐）和 'nearest'（最近对齐）。
                        inline：指定元素在水平方向上的对齐方式，可取的值有 'start'、'center'、'end' 和 'nearest'。
                    */

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

                }catch (Exception e){
                    e.printStackTrace();
                    //效果：X轴和Y轴都滚动到屏幕的顶部
                    chromeDriver.executeScript("arguments[0].scrollIntoView();", we);
                }

                handleContext.setDetail("控件元素 " + selector + ":" + pathValue + " 滚动至页面顶部");
            }

        } catch (Exception e) {
            handleContext.setE(e);
        }

    }



    /**
     * 迭代webview元素
     * @param androidStepHandler
     * @param chromeDriver
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     */
    public static void iteratorWebviewElement(AndroidStepHandler androidStepHandler,ChromeDriver chromeDriver,HandleContext handleContext,String des, String selector, String pathValue){
//TODO 后续扩展
        //        List<WebElement> webElements = null;
//
//        if (handleContext.iteratorWebElement == null) {
//            handleContext.setStepDes("迭代webview控件列表 " + des);
//            try {
//                webElements = androidStepHandler.findWebEleList(selector, pathValue);
//                handleContext.iteratorWebElement = webElements.iterator();
//            } catch (Throwable e) {
//                handleContext.setE(e);
//                return;
//            }
//            androidStepHandler.getLog().sendStepLog(StepType.INFO, "", "迭代webview控件列表长度：" + webElements.size());
//        }
//
//        if (handleContext.iteratorWebElement.hasNext()) {
//            handleContext.currentIteratorWebElement = handleContext.iteratorWebElement.next();
//            WebElement w = (WebElement) handleContext.currentIteratorWebElement;
//            try {
//                handleContext.setStepDes("当前迭代webview控件："+w.getTagName()+" "+w.getText()+" "+w.getAttribute("src"));
//                handleContext.setDetail("");
//            } catch (Exception e) {
//                handleContext.setStepDes("当前迭代webview控件："+w.getTagName());
//                handleContext.setDetail("");
//            }
//
//        } else {
//            handleContext.iteratorWebElement = null;
//            handleContext.currentIteratorWebElement = null;
//            androidStepHandler.getLog().sendStepLog(StepType.INFO, "", "迭代webview控件列表完毕...");
//            handleContext.setStepDes("迭代webview控件列表 " + des);
//            handleContext.setE(new Exception("exit while"));
//        }
    }


    /**
     * 模拟手势事件，up、down、move
     * @param androidStepHandler
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     * @param motionEventType
     * @throws SonicRespException
     */
    public static void motionEventByEle(AndroidStepHandler androidStepHandler,HandleContext handleContext, String des, String selector, String pathValue, String motionEventType) throws SonicRespException {
//TODO 后续扩展
        //        try {
//            AndroidElement webElement = androidStepHandler.findEle(selector, pathValue);
//            int x = webElement.getRect().getX() + webElement.getRect().getWidth() / 2;
//            int y = webElement.getRect().getY() + webElement.getRect().getHeight() / 2;
//            handleContext.setStepDes("通过" + des + "触发" + motionEventType + "-Motion事件");
//            handleContext.setDetail("对坐标" + x + "," + y + String.format("执行Motion事件(%s)", motionEventType));
//            //input motionevent
//            AndroidTouchHandler.motionEvent(androidStepHandler.getiDevice(), motionEventType, x, y);
//        } catch (SonicRespException e) {
//            handleContext.setE(e);
//        }
    }


    /**
     * 持续滑动，一直到指定元素可见
     * 源代码只支持up 和 down，  扩展支持 left 和 right 滑动。并且原版本的 up 和 down 滑动是针对整个屏幕的，而不是针对元素的。
     * 扩展内容：
     * 1、up、down、left、right：如果控件pathvalue中携带了自定义的 @P 父元素，那么滑动时就是针对父元素滑动，不是针对整个屏幕。
     * 2、如果没有携带@P 父元素，up和down保持针对整个屏幕滑动，left和right提示用户不合法
     * @param androidStepHandler
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     * @param maxTryTime
     * @param direction
     * @throws Exception
     */
    public static void scrollToEle(AndroidStepHandler androidStepHandler, HandleContext handleContext, String des, String selector, String pathValue, int maxTryTime, String direction) throws Exception {
        if(pathValue.contains(FGF)){
            String p = (pathValue.split(FGFRegex))[0].trim();
            String extra = (pathValue.split(FGFRegex))[1].trim();
            ////android.widget.TextView[@resource-id='com.sinovatech.unicom.ui:id/jingangqu_text' and @text='自助排障'] || @p=//android.support.v7.widget.RecyclerView[@resource-id='com.sinovatech.unicom.ui:id/jingangqu_recycleview']
            if(extra.startsWith("@p=")){
                String parentPath = extra.replace("@p=","");
                //获取父元素
                AndroidElement parentElement = androidStepHandler.findEle(selector, parentPath);
                if (parentElement!= null) {
                    int x = parentElement.getRect().getX();
                    int y = parentElement.getRect().getY();
                    int width = parentElement.getRect().getWidth();
                    int height = parentElement.getRect().getHeight();

                    if ("up".equals(direction)) {
                        //滚动 input swipe
                        AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), x + width/2, y + height * 2/3, x + width/2, y, 1000);
                    }else if ("down".equals(direction)) {
                        //滚动 input swipe
                        AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), x + width/2, y, x + width/2, y + height * 2/3, 1000);

                    }else if ("left".equals(direction)) {
                        //滚动 input swipe
                        AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), x + width * 2/3, y + height / 2, x , y + height / 2, 1000);

                    }else if ("right".equals(direction)) {
                        //滚动 input swipe
                        AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), x, y + height / 2, x + width * 2/3, y + height / 2, 1000);

                    }else{
                        handleContext.setE(new Exception("未知方向类型设置或者不支持该方向的滚动"));
                        throw new RuntimeException("exit while");
                    }

                }
            }else{
                throw new RuntimeException("exit while");
            }

        }else{
            final int xOffset = 20;
            //按原有逻辑处理
            if ("up".equals(direction)) {
                AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), xOffset, androidStepHandler.screenHeight * 2 / 3, xOffset, androidStepHandler.screenHeight / 3, 1000);

            } else if ("down".equals(direction)) {
                AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), xOffset, androidStepHandler.screenHeight / 3, xOffset, androidStepHandler.screenHeight * 2 / 3, 1000);

            } else {
                handleContext.setE(new Exception("未知方向类型设置或者不支持该方向的滚动"));
                throw new RuntimeException("exit while");
            }
        }
    }



    /**
     * 在控件元素内部，上下左右滑动指定的距离
     * @param androidStepHandler
     * @param handleContext
     * @param des
     * @param selector
     * @param pathValue
     * @param maxTryTime
     * @param direction
     * @throws Exception
     */
    public static void swipeByDefinedDirectionInElement(AndroidStepHandler androidStepHandler, HandleContext handleContext, String des, String selector, String pathValue, String direction, int distance) throws Exception {
        AndroidElement element = androidStepHandler.findEle(selector, pathValue);
        int x = element.getRect().getX();
        int width = element.getRect().getWidth();
        int y = element.getRect().getY();
        int height = element.getRect().getHeight();
        int weight = 150;
        if(y<weight){
            //如果元素在屏幕顶部，避免触碰到状态栏，做一下容错处理
            y = y + weight;
            height = height - weight;
            if(height<=0){
                throw new Exception("控件元素顶部过于靠近手机状态栏，容易误操作，控件元素高度小于"+weight+"px，无法完成滑动操作");
            }
        }

        switch (direction) {
            case "up" -> {
                handleContext.setStepDes("在"+des+"内向上滑动" + distance + "像素");
                int startY = (y + distance) > height ? (y+height) : (y + distance);
                int endY = y;
                int centerX = x + width / 2;
                try {
                    AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), centerX, startY, centerX, endY);
                } catch (Exception e) {
                    handleContext.setE(e);
                }
                handleContext.setDetail("拖动坐标(" + centerX + "," + startY + ")到(" + centerX + "," + endY + ")");
            }
            case "down" -> {
                handleContext.setStepDes("在"+des+"内向下滑动" + distance + "像素");
                int startY = y;
                int endY = (y + distance) > height ? (y+height) : (y + distance);
                int centerX = x + width / 2;
                try {
                    AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), centerX, startY, centerX, endY);
                } catch (Exception e) {
                    handleContext.setE(e);
                }
                handleContext.setDetail("拖动坐标(" + centerX + "," + startY + ")到(" + centerX + "," + endY + ")");
            }
            case "left" -> {
                handleContext.setStepDes("在"+des+"内向左滑动" + distance + "像素");
                int startX = (x + distance) > width ? (x+width) : (x + distance);
                int endX = x;
                int centerY = y + width / 2;
                try {
                    AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), startX, centerY, endX, centerY);
                } catch (Exception e) {
                    handleContext.setE(e);
                }
                handleContext.setDetail("拖动坐标(" + startX + "," + centerY + ")到(" + endX + "," + centerY + ")");
            }
            case "right" -> {
                handleContext.setStepDes("在"+des+"内向右滑动" + distance + "像素");
                int startX = x;
                int endX = (x + distance) > width ? (x+width) : (x + distance);
                int centerY = y + width / 2;
                try {
                    AndroidTouchHandler.swipe(androidStepHandler.getiDevice(), startX, centerY, endX, centerY);
                } catch (Exception e) {
                    handleContext.setE(e);
                }
                handleContext.setDetail("拖动坐标(" + startX + "," + centerY + ")到(" + endX + "," + centerY + ")");
            }
            default ->
                    throw new Exception("Sliding in this direction is not supported. Only up/down/left/right are supported!");
        }
    }



}



