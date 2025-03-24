package org.cloud.sonic.agent.tests.handlers;

import com.alibaba.fastjson2.JSONObject;
import org.cloud.sonic.agent.common.models.HandleContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

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
     *
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
            if(type.equals("css::after")){
                //解析目标 x和y值 作为点击的坐标
                int x = jo.getIntValue("x",0);
                int y = jo.getIntValue("y",0);
                WebElement element = androidStepHandler.findWebEle(selector, pathValue);
                int target_X = element.getLocation().getX() + x;
                int target_Y = element.getLocation().getY() + y;

                handleContext.setStepDes(StepTitle);
                handleContext.setDetail("处理::after类型的元素，首先找到它的父元素【"+pathValue+"】，点击相对于父元素的坐标（"+x+","+y+"），点击相对于屏幕原点的坐标（"+target_X+","+target_Y+"）");

                chromeDriver.executeScript("document.elementFromPoint(arguments[0],arguments[1]).click()",target_X,target_Y);
            }


        }else{
            //按原来的规范点击
            androidStepHandler.findWebEle(selector,pathValue).click();
        }
    }

}



