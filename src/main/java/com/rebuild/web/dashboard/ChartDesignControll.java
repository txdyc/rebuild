/*
rebuild - Building your system freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BadParameterException;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 12/09/2018
 */
@Controller
@RequestMapping("/dashboard")
public class ChartDesignControll extends BaseControll {

	@RequestMapping("/chart-design")
	public ModelAndView pageHome(HttpServletRequest request) {
		ModelAndView mv = createModelAndView("/dashboard/chart-design.jsp");
		
		String entity = getParameter(request, "source");
		ID chartId = getIdParameter(request, "id");
		
		Entity entityMeta = null;
		if (chartId != null) {
			Object[] config = Application.createQuery(
					"select belongEntity,title,config from ChartConfig where chartId = ?")
					.setParameter(1, chartId)
					.unique();
			mv.getModel().put("chartId", chartId);
			mv.getModel().put("chartTitle", config[1]);
			mv.getModel().put("chartConfig", config[2]);
			
			entityMeta = MetadataHelper.getEntity((String) config[0]);
		} else if (entity != null) {
			mv.getModel().put("chartConfig", "{}");
			entityMeta = MetadataHelper.getEntity(entity);
		} else {
			throw new BadParameterException();
		}
		
		putEntityMeta(mv, entityMeta);

		List<String[]> fields = new ArrayList<>();
		for (Field field : MetadataSorter.sortFields(entityMeta)) {
			EasyMeta easy = EasyMeta.valueOf(field);
			DisplayType dt = easy.getDisplayType();
			String type = "text";
			if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
				type = "date";
			} else if (dt == DisplayType.NUMBER || dt == DisplayType.DECIMAL) {
				type = "num";
			}
			fields.add(new String[] { easy.getName(), easy.getLabel(), type });
		}
		mv.getModel().put("fields", fields);
		
		return mv;
	}
	
	@RequestMapping("/chart-save")
	public void chartSave(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		ID dashid = null;
		if (record.getPrimary() == null) {
			dashid = getIdParameterNotNull(request, "dashid");
		}
		record = Application.getCommonService().createOrUpdate(record);
		
		// 添加到仪表盘
		if (dashid != null) {
			Object[] dash = Application.createQuery(
					"select config from DashboardConfig where dashboardId = ?")
					.setParameter(1, dashid)
					.unique();
			JSONArray config = JSON.parseArray((String) dash[0]);
			
			JSONObject item = JSONUtils.toJSONObject("chart", record.getPrimary().toLiteral());
			config.add(item);
			
			Record record2 = EntityHelper.forUpdate(dashid, getRequestUser(request));
			record2.setString("config", config.toJSONString());
			Application.getCommonService().createOrUpdate(record2);
		}
		
		JSONObject ret = JSONUtils.toJSONObject("id", record.getPrimary().toLiteral());
		writeSuccess(response, ret);
	}
}
