package org.jeecg.modules.python;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.entity.Community;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.spring.web.json.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/python")
public class Demo1 {


    @RequestMapping(value = "/chuanzhi", method = RequestMethod.GET)
    public String[] chuanzhi() {
        String [] str=  new String [2];
        str = yunxingpython();
        str[0] = str[0].replaceAll("\'","\"");
        str[1] = str[1].replaceAll("\'","\"");
        return str;
    }

    public static void main(String[] args) {
        String [] str = yunxingpython();
        System.out.println(str[0]);
        System.out.println(str[1]);

    }

    public static String []yunxingpython(){
        Process proc;
        String [] str = new String[2];
        try {
            proc = Runtime.getRuntime().exec("python C:\\Users\\60361\\PycharmProjects\\pythonProject\\jisuan.py");//执行Py文件
            //用输入输出流来截取结果
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = in.readLine();
            str[0] = line;
//            System.out.println(str[0]);
//            System.out.println(line);
            String line1 = in.readLine();
            str[1] = line1;
//            System.out.println(str[1]);
//            System.out.println(line1);
            in.close();
            proc.waitFor();
            return str;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return str;
    }
}
