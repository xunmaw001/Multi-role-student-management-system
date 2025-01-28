package com.controller;


import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.StringUtil;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;

import com.entity.ChengjiEntity;

import com.service.ChengjiService;
import com.entity.view.ChengjiView;
import com.service.YonghuService;
import com.entity.YonghuEntity;
import com.utils.PageUtils;
import com.utils.R;

/**
 * 成绩
 * 后端接口
 * @author
 * @email
 * @date 2021-03-02
*/
@RestController
@Controller
@RequestMapping("/chengji")
public class ChengjiController {
    private static final Logger logger = LoggerFactory.getLogger(ChengjiController.class);

    @Autowired
    private ChengjiService chengjiService;


    @Autowired
    private TokenService tokenService;


    //级联表service
    @Autowired
    private YonghuService yonghuService;

    //字典表map
    Map<String, Map<Integer, String>> dictionaryMap;

    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
    logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isNotEmpty(role) && ("学生".equals(role) ||"班干部".equals(role) )){
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        }
    PageUtils page = chengjiService.queryPage(params);

    //字典表数据转换
    List<ChengjiView> list =(List<ChengjiView>)page.getList();
        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        dictionaryMap = (Map<String, Map<Integer, String>>) servletContext.getAttribute("dictionaryMap");
        for(ChengjiView c:list){
        this.dictionaryConvert(c);
        }
        return R.ok().put("data", page);
    }
    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ChengjiEntity chengji = chengjiService.selectById(id);
        if(chengji !=null){
            //entity转view
            ChengjiView view = new ChengjiView();
            BeanUtils.copyProperties( chengji , view );//把实体数据重构到view中

            //级联表
            YonghuEntity yonghu = yonghuService.selectById(chengji.getYonghuId());
            if(yonghu != null){
                BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setYonghuId(yonghu.getId());
            }
            //字典表字典转换
            ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
            dictionaryMap = (Map<String, Map<Integer, String>>) servletContext.getAttribute("dictionaryMap");
            this.dictionaryConvert(view);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ChengjiEntity chengji, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,chengji:{}",this.getClass().getName(),chengji.toString());
        Wrapper<ChengjiEntity> queryWrapper = new EntityWrapper<ChengjiEntity>()
            .eq("yonghu_id", chengji.getYonghuId())
            .eq("exam_name", chengji.getExamName())
            .eq("kemu_types", chengji.getKemuTypes())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ChengjiEntity chengjiEntity = chengjiService.selectOne(queryWrapper);
        if(chengjiEntity==null){
            chengji.setCreateTime(new Date());
        //  String role = String.valueOf(request.getSession().getAttribute("role"));
        //  if("".equals(role)){
        //      chengji.set
        //  }
            chengjiService.insert(chengji);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ChengjiEntity chengji, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,chengji:{}",this.getClass().getName(),chengji.toString());
        //根据字段查询是否有相同数据
        Wrapper<ChengjiEntity> queryWrapper = new EntityWrapper<ChengjiEntity>()
            .notIn("id",chengji.getId())
            .eq("yonghu_id", chengji.getYonghuId())
            .eq("exam_name", chengji.getExamName())
            .eq("kemu_types", chengji.getKemuTypes())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ChengjiEntity chengjiEntity = chengjiService.selectOne(queryWrapper);
        if(chengjiEntity==null){
            //  String role = String.valueOf(request.getSession().getAttribute("role"));
            //  if("".equals(role)){
            //      chengji.set
            //  }
            chengjiService.updateById(chengji);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }


    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        chengjiService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }

    /**
    *字典表数据转换
    */
    public void dictionaryConvert(ChengjiView chengjiView){
        //当前表的字典字段
        if(StringUtil.isNotEmpty(String.valueOf(chengjiView.getKemuTypes()))){
            chengjiView.setKemuValue(dictionaryMap.get("kemu_types").get(chengjiView.getKemuTypes()));
        }

        //级联表的字典字段
        if(StringUtil.isNotEmpty(String.valueOf(chengjiView.getSexTypes()))){
            chengjiView.setSexValue(dictionaryMap.get("sex_types").get(chengjiView.getSexTypes()));
        }
        if(StringUtil.isNotEmpty(String.valueOf(chengjiView.getPoliticsTypes()))){
            chengjiView.setPoliticsValue(dictionaryMap.get("politics_types").get(chengjiView.getPoliticsTypes()));
        }
        if(StringUtil.isNotEmpty(String.valueOf(chengjiView.getYonghuTypes()))){
            chengjiView.setYonghuValue(dictionaryMap.get("yonghu_types").get(chengjiView.getYonghuTypes()));
        }
    }


}

