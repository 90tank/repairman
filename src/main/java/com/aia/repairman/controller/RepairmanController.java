package com.aia.repairman.controller;

import java.util.Arrays;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aia.repairman.service.RepairmanService;
import com.aia.repairman.util.ClientResult;

@RestController
public class RepairmanController {

	@Autowired
	private RepairmanService repairmanService;

	@RequestMapping(value = "/db2/autoConvertBeforeFixedSql", method = RequestMethod.POST)
	public ClientResult handelConvertBeforeFixed(@RequestParam("sourceDirFile") String sourceDirFile,
			@RequestParam("targetPath") String targetPath, @RequestParam("fixTypes") String fixTypes) {
		String[] typeArray = fixTypes.split(",");
		HashSet<String> typeSet = new HashSet<String>(Arrays.asList(typeArray));
		ClientResult handelSql = repairmanService.handelConvertBeforeFixed(sourceDirFile, targetPath, typeSet);
		return handelSql;
	}

	@RequestMapping(value = "/db2/autoConvertAfterFixedSql", method = RequestMethod.POST)
	public ClientResult handelConvertAfterFixed(@RequestParam("fixTypes") String fixTypes,
			@RequestParam("sourceDirFile") String sourceDirFile, @RequestParam("targetPath") String targetPath) {
		String[] typeArray = fixTypes.split(",");
		HashSet<String> typeSet = new HashSet<String>(Arrays.asList(typeArray));
		ClientResult handelSql = repairmanService.handelConvertAfterFixed(sourceDirFile, targetPath, typeSet);
		return handelSql;
	}

	@RequestMapping(value = "/db2/test", method = RequestMethod.GET)
	public String handelConvertAfterFixed1(String driFile, String targetPath) {
		return "hello world";
	}
}
