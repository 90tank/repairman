package com.aia.repairman.service.impl;

import cn.hutool.core.util.ReUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.aia.repairman.service.RepairmanService;
import com.aia.repairman.sybase.comment.CommentConvertUtil;
import com.aia.repairman.util.ClientResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service("sybase2ProcedureService")
@Slf4j
public class RepairmanServiceImpl implements RepairmanService {

	/*
	 *SUCCESS 
	 */
	private static int operateSuccess=0;
	
	/**
	 * Fail
	 */
	private static int operateFail=1;
	
    @Override
    public ClientResult handelConvertBeforeFixed(String fileName,String targetPath) {
    	ClientResult result=new ClientResult();
    	//comment fix
    	String fileCommentPath=handelCommentFixed(result,fileName,targetPath);
    	if(result.getState()!=0) {
    		return result;
    	}
    	//create table before 
    	String fileCreatTable1=creatTableBefore(fileCommentPath,targetPath,result);
    	if(result.getState()!=0) {
    		return result;
    	}
    	
    	//deleteTailReplaceFileHead
    	String deleteTailPath=deleteTailReplaceFileHead(fileCreatTable1,targetPath,result);
    	if(result.getState()!=0) {
    		return result;
    	}   	
    	return result;
    }
    
    /**
     * Comment question
     * 
     * @param fileName
     * @return
     */
    private String handelCommentFixed(ClientResult result,String fileName,String targetPath) {
    	File targetNameFile= new File(targetPath);
    	if(!targetNameFile.exists()) {
    		result.setState(2);
    		result.setOperation("Target path does not exist(Comment)");
    		log.error("Target path does not exist(Comment)");
    		return null;
    	}
    	//创建目标目录
		File resultFile=new File(targetNameFile.getParent()+File.separator+"comment");
		isExit(resultFile);
		
		File sourcefile=new File(fileName);
	    FileReader filerReader=null;
        FileWriter writer=null;
		if(sourcefile.exists()&&sourcefile.isDirectory()) {
			File[] files=sourcefile.listFiles();
			if(files!=null&&files.length>0) {
				for(int i=0;i<files.length;i++) {
					if(!files[i].isFile()) {
						continue;
					}
					try {
						filerReader = new FileReader(files[i]);
					} catch (FileNotFoundException e) {
				 		result.setState(1);
			    		result.setOperation("source file not find(Comment)");
			    		result.setErrorMessage(e.getMessage());
			    		log.error("source file not find(Comment),Exception:"+e.getMessage());
					}
					try {
						writer = new FileWriter(resultFile+File.separator+files[i].getName());
					} catch (IOException e) {
						result.setState(2);
			    		result.setOperation("FileWriter is error(Comment)");
			    		result.setErrorMessage(e.getMessage());
			    		log.error("FileWriter is error(Comment),Exception:"+e.getMessage());
					}


			        try {
						int pushBackLimit = 5;
						PushbackReader reader = new PushbackReader(filerReader,pushBackLimit);
						CommentConvertUtil convertUtil = new CommentConvertUtil();
						convertUtil.blockComment2SingleLineComment(reader, writer);
					} catch (IOException e) {
						result.setState(2);
			    		result.setOperation("reader and writer are error(Comment)");
			    		result.setErrorMessage(e.getMessage());
			    		log.error("reader and writer are error,Exception:"+e.getMessage());
					}

				}
			}else {
				result.setState(1);
	    		result.setOperation("No files in source directory(comment)");
	    		log.error("No files in source directory(comment)");
			}
		}else {
			result.setState(1);
    		result.setOperation("The source file is not a directory or does not exist(comment)");
    		log.error("The source file is not a directory or does not exist(comment)");
		}
		return resultFile.getAbsolutePath();
    }

    /**
     * 
     * @param sql
     * @param fileName
     * @throws IOException
     */
    public void sqlOutWritter(String sql,String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("E:\\sqlfixed\\after\\comment\\"+fileName+".sql"));
        writer.write(sql);
        writer.close();
    }
    
    /**
     * create table question
     * @param path
     * @return
     */
	private String creatTableBefore(String path,String targetPath,ClientResult result) {
		File targetNameFile= new File(targetPath);
    	if(!targetNameFile.exists()) {
    		result.setState(2);
    		result.setOperation("Target path does not exist(creatTable)");
    		log.error("Target path does not exist(creatTable)");
    		return null;
    	}
		//创建目标目录
		File resultFile=new File(targetNameFile.getParent()+File.separator+"creatTableBefore");
		isExit(resultFile);

		FileReader filerReader = null;
		FileWriter writer = null;
		BufferedReader br = null;
		File file=new File(path);
		if(file.exists()&&file.isDirectory()) {
			File[] files=file.listFiles();
			if(files!=null&&files.length>0) {
				for(int i=0;i<files.length;i++) {
					if(files[i].isDirectory()) {
						continue;
					}
					try {
						filerReader = new FileReader(files[i]);
						writer = new FileWriter(resultFile.getAbsolutePath()+File.separator+files[i].getName());
						br = new BufferedReader(filerReader);
						String fengli = "(\\b(?i)create\\b)\\s+((?i)table)(\\s+)(?<tableName>[^ ^\\(]+)"; // 提取风级的正则表达式
						Pattern pafengli = Pattern.compile(fengli);
						while (true) {
							String line = br.readLine();
							if (line == null) {
								break;
							}
							Matcher matfengli = pafengli.matcher(line);
							while (matfengli.find()) {
								String cutstr = matfengli.group("tableName");
								if (!cutstr.contains("#")) {
									String resultCutstr = "#jiamingyun" + cutstr.replace(".", "$");
									String line2 = line.replace(cutstr, resultCutstr);
									line = line2;
								}
							}
							line = line + '\n';
							writer.append(line);
						}
					} catch (Exception e) {
						result.setState(1);
			        	result.setOperation("File write or read error(creat table before)");
			        	log.error("File write or read error:"+e.getMessage());
					} finally {
						if (br != null) {
							try {
								br.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("BufferedReader read error(creat table before)");
					        	log.error("BufferedReader read error:"+e.getMessage());
							}
						}
						if (filerReader != null) {
							try {
								filerReader.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("FileReader read error(creat table before)");
					        	log.error("FileReader read error:"+e.getMessage());
							}
						}
						if (writer != null) {
							try {
								writer.close();
							} catch (IOException e) {
								result.setState(2);
					        	result.setOperation("FileWriter read error(creat table before)");
					        	log.error("FileWriter read error:"+e.getMessage());
							}
						}
					}
				}
			}else {
				result.setState(1);
	        	result.setOperation("No files in the directory(creat table before)");
	        	log.error("No files in the directory");
			}
		}else {
			result.setState(1);
        	result.setOperation("The source file path cannot be non-existent or non-folder(creat table before)");
        	log.error("The source file path cannot be non-existent or non-folder(creat table before)");
		}
		return resultFile.getAbsolutePath();

	}
	
	/**
	 * 
	 * @return
	 */
	private String deleteTailReplaceFileHead(String sourceFile,String tagertFile,ClientResult result) {
		File targetNameFile= new File(tagertFile);
    	if(!targetNameFile.exists()) {
    		result.setState(2);
    		result.setOperation("Target path does not exist(deleteTailReplaceFileHead)");
    		log.error("Target path does not exist(deleteTailReplaceFileHead)");
    		return null;
    	}
		FileReader filerReader = null;
		FileWriter writer = null;
		BufferedReader br = null;
		File file=new File(sourceFile);
		if(file.exists()&&file.isDirectory()) {
			File[] files=file.listFiles();
			if(files!=null&&files.length>0) {
				for(int i=0;i<files.length;i++) {
					if(files[i].isDirectory()) {
						continue;
					}
					try {
						filerReader = new FileReader(files[i]);
						writer = new FileWriter(targetNameFile.getAbsoluteFile()+File.separator+files[i].getName());
						br = new BufferedReader(filerReader);
						while (true) {
							String line = br.readLine();
							if (line == null) {
								break;
							}
							if (line.contains("syb_quit()")) {
								line = line.replaceAll("((?i)select)\\s+(syb_quit\\(\\))", "return");
							} else {
								if (line.matches(".*((?i)sp_procxmode).*")) {
									break;
								}
							}
							line = line + '\n';
							writer.append(line);
						}
					} catch (Exception e) {
						result.setState(1);
			        	result.setOperation("FileReader or FileReader is error(deleteTailReplaceFileHead)");
			        	log.error("FileReader or FileReader is error(deleteTailReplaceFileHead)");
					} finally {
						if (br != null) {
							try {
								br.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("BufferedReader is error(deleteTailReplaceFileHead)");
					        	log.error("BufferedReaderis error(deleteTailReplaceFileHead)");
							}
						}
						if (filerReader != null) {
							try {
								filerReader.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("FileReader is error(deleteTailReplaceFileHead)");
					        	log.error("FileReader is error(deleteTailReplaceFileHead)");
							}
						}
						if (writer != null) {
							try {
								writer.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("FileWriter is error(deleteTailReplaceFileHead)");
					        	log.error("FileWriter is error(deleteTailReplaceFileHead)");
							}
						}
					}
				}
			}else {
				result.setState(1);
	        	result.setOperation("No files in the directory(deleteTailReplaceFileHead)");
	        	log.error("No files in the directory(deleteTailReplaceFileHead)");
			}
		}else {
			result.setState(1);
        	result.setOperation("The source file path cannot be non-existent or non-folder(deleteTailReplaceFileHead)");
        	log.error("The source file path cannot be non-existent or non-folder(deleteTailReplaceFileHead)");
		}
		return targetNameFile.getAbsolutePath();
	}
	

	/**
	 * Check if the file exists, not create
	 */
	private void isExit(File file) {
		if(file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			log.error("Failed to create directory:"+e.getMessage());
		}
	}
    /**
     * 转换后fix文件
     */
	@Override
	public ClientResult handelConvertAfterFixed(String sourcePath,String targetPath) {
		ClientResult result=new ClientResult();
	   	//create table After 
    	String fileCreatTable1=creatTableAfter(sourcePath,targetPath,result);
    	if(result.getState()!=0) {
    		return result;
    	}
		return result;
	}
	
	private String creatTableAfter(String sourcePath,String targetPath,ClientResult result) {
    	File targetNameFile= new File(targetPath);
    	if(!targetNameFile.exists()) {
    		result.setState(2);
    		result.setOperation("Target path does not exist(creatTableAfter)");
    		log.error("Target path does not exist(creatTableAfter)");
    		return null;
    	}
    	//创建目标目录
		File resultFile=new File(targetNameFile.getParent()+File.separator+"creatTableAfter");
		isExit(resultFile);

		FileReader filerReader = null;
		FileWriter writer = null;
		BufferedReader br = null;
		File file=new File(sourcePath);
		if(file.exists()&&file.isDirectory()) {
			File[] files=file.listFiles();
			if(files!=null&&files.length>0) {
				for(int i=0;i<files.length;i++) {
					if(files[i].isDirectory()) {
						continue;
					}
					try {
						filerReader = new FileReader(files[i]);
						writer = new FileWriter(resultFile+File.separator+files[i].getName());
						br = new BufferedReader(filerReader);
						String fengli = "(\\b(?i)create\\b)\\s+((?i)table)(\\s+)(?<tableName>[^ ^\\(]+)"; // 提取风级的正则表达式
						Pattern pafengli = Pattern.compile(fengli);

						while (true) {
							String line = br.readLine();
							if (line == null) {
								break;
							}
							Matcher matfengli = pafengli.matcher(line);
							while (matfengli.find()) {
								String cutstr = matfengli.group("tableName");
								if (cutstr.contains("#jiamingyun")) {
									String cutstrTmp = cutstr.replace("#jiamingyun", "");
									String resultCutstr = cutstrTmp.replace("$", ".");
									String line2 = line.replace(cutstr, resultCutstr);
									line = line2;
								}
							}
							line = line + '\n';
							writer.append(line);
						}
					} catch (Exception e) {
						result.setState(1);
			        	result.setOperation("FileReader and FileWriter error(creatTableAfter)");
			        	log.error("FileReader and FileWriter error(creatTableAfter) : "+e.getMessage());
					} finally {
						if (br != null) {
							try {
								br.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("FileReader and FileWriter error(creatTableAfter)");
					        	log.error("FileReader and FileWriter error(creatTableAfter) : "+e.getMessage());
							}
						}
						if (filerReader != null) {
							try {
								filerReader.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("FileReader close error(creatTableAfter)");
					        	log.error("FileReader close error(creatTableAfter) : "+e.getMessage());
							}
						}
						if (writer != null) {
							try {
								writer.close();
							} catch (IOException e) {
								result.setState(1);
					        	result.setOperation("FileWriter close error(creatTableAfter)");
					        	log.error("FileWriter close error(creatTableAfter) : "+e.getMessage());
							}
						}
					}
				}
			}else {
				result.setState(1);
	        	result.setOperation("The source files not find(creatTableAfter)");
	        	log.error("The source files not find(creatTableAfter) : ");
			}
		}else {
			result.setState(1);
        	result.setOperation("The source file path cannot be non-existent or non-folder(creatTableAfter)");
        	log.error("The source file path cannot be non-existent or non-folder(creatTableAfter) : ");
		}
		return resultFile.getAbsolutePath();
	}
	

	@Override
	public ClientResult handelSynchronizeFixed(String dirName) {

		return null;
	}

}