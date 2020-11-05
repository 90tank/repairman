package com.aia.repairman.service;

import java.io.IOException;
import java.util.HashSet;

import com.aia.repairman.util.ClientResult;

public interface RepairmanService {
    ClientResult handelConvertBeforeFixed(String dirName,String targetPath,HashSet<String> typeSet);
    ClientResult handelConvertAfterFixed(String dirName,String targetPath,HashSet<String> types);
    ClientResult handelSynchronizeFixed(String dirName);
}
