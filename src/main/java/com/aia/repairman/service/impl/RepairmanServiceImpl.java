package com.aia.repairman.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.aia.repairman.service.RepairmanService;
import com.aia.repairman.sqlserver.RepairManConstant;
import com.aia.repairman.sqlserver.RepariManFixType;
import com.aia.repairman.sqlserver.S2ssUdf;
import com.aia.repairman.sybase.comment.CommentConvertUtil;
import com.aia.repairman.util.ClientResult;

import lombok.extern.slf4j.Slf4j;

@Service("sybase2ProcedureService")
@Slf4j
public class RepairmanServiceImpl implements RepairmanService {

	/*
	 * SUCCESS
	 */
	private static int operateSuccess = 0;

	/**
	 * Fail=1与原文件有关,=2与输出文件有关
	 */
	private static int OPERATEFAIL1 = 1;

	private static int OPERATEFAIL2 = 2;

	@Override
	public ClientResult handelConvertBeforeFixed(String filePath, String targetPath, HashSet<String> typeSet) {
		ClientResult result = new ClientResult();
		boolean fixFlag = false;
		String tmpPath = filePath;
		// 1. comment fix
		if (typeSet.contains(RepariManFixType.COMMENT)) {
			tmpPath = handelCommentFixed(result, tmpPath, targetPath);
			if (result.getState() != 0) {
				return result;
			}
			fixFlag = true;
		}
		// 2. create table before
		if (typeSet.contains(RepariManFixType.TABLE)) {
			tmpPath = creatTableBefore(tmpPath, targetPath, result);
			if (result.getState() != 0) {
				return result;
			}
			fixFlag = true;
		}
		// 3. deleteTailReplaceFileHead
		if (typeSet.contains(RepariManFixType.COMMON_HEADER_TAIL)) {
			tmpPath = deleteTailReplaceFileHead(tmpPath, targetPath, result);
			if (result.getState() != 0) {
				return result;
			}
			fixFlag = true;
		}

		if (fixFlag) {
			// copy结果文件
			try {
				copyDir(tmpPath, targetPath + File.separator + "result");
			} catch (IOException e) {
				log.error("IOException when copy ", e);
			}
		}

		return result;
	}

	/**
	 * Comment question
	 * 
	 * @param filePath
	 * @return
	 */
	private String handelCommentFixed(ClientResult result, String filePath, String targetPath) {
		File targetNameFile = new File(targetPath);
		if (!targetNameFile.exists()) {
			result.setState(OPERATEFAIL2);
			result.setOperation("Target path does not exist(Comment)");
			log.error("Target path does not exist(Comment)");
			return null;
		}
		File sourcefile = new File(filePath);
		FileReader filerReader = null;
		FileWriter writer = null;
		if (sourcefile.exists() && sourcefile.isDirectory()) {
			// DB DIR
			File[] files = sourcefile.listFiles();
			if (files != null && files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isFile()) {
						continue;
					} else {
						String dbName = files[i].getName();
						String errorPath = "";
						if (!filePath.contains("process")) {
							errorPath = files[i].getAbsolutePath() + File.separator + "StoreProcedure" + File.separator
									+ "error" + File.separator + "origin";
						} else {
							errorPath = files[i].getAbsolutePath();
						}

						File errorFiles = new File(errorPath);
						// 创建目标目录
						File resultFile = new File(targetNameFile.getAbsolutePath() + File.separator + "process"
								+ File.separator + "comment" + File.separator + dbName);
						isExit(resultFile);
						if (errorFiles.exists() && errorFiles.isDirectory()) {
							File[] lists = errorFiles.listFiles();
							for (int j = 0; j < lists.length; j++) {
								try {
									filerReader = new FileReader(lists[j]);
								} catch (FileNotFoundException e) {
									result.setState(OPERATEFAIL1);
									result.setOperation("source file not find(Comment)");
									result.setErrorMessage(e.getMessage());
									log.error("source file not find(Comment),Exception:" + e.getMessage());
								}
								try {
									writer = new FileWriter(resultFile + File.separator + lists[j].getName());
								} catch (IOException e) {
									result.setState(OPERATEFAIL2);
									result.setOperation("FileWriter is error(Comment)");
									result.setErrorMessage(e.getMessage());
									log.error("FileWriter is error(Comment),Exception:" + e.getMessage());
								}
								int pushBackLimit = 5;
								PushbackReader reader = new PushbackReader(filerReader, pushBackLimit);
								CommentConvertUtil convertUtil = new CommentConvertUtil();
								try {
									convertUtil.blockComment2SingleLineComment(reader, writer);
								} catch (IOException e) {
									result.setState(OPERATEFAIL2);
									result.setOperation("reader and writer are error(Comment)");
									result.setErrorMessage(e.getMessage());
									log.error("reader and writer are error,Exception:" + e.getMessage());
								}
							}
						} else {
							result.setState(OPERATEFAIL1);
							result.setOperation("source file not find(Comment)");
							log.error("source file not find(Comment)");
						}
					}
				}
			} else {
				result.setState(OPERATEFAIL1);
				result.setOperation("No files in source directory(comment)");
				log.error("No files in source directory(comment)");
			}
		} else {
			result.setState(OPERATEFAIL1);
			result.setOperation("The source file is not a directory or does not exist(comment)");
			log.error("The source file is not a directory or does not exist(comment)");
		}
		return targetNameFile.getAbsolutePath() + File.separator + "process" + File.separator + "comment";
	}

	/**
	 * create table question
	 * 
	 * @param path
	 * @return
	 */
	private String creatTableBefore(String path, String targetPath, ClientResult result) {
		File targetNameFile = new File(targetPath);
		if (!targetNameFile.exists()) {
			result.setState(OPERATEFAIL2);
			result.setOperation("Target path does not exist(creatTable)");
			log.error("Target path does not exist(creatTable)");
			return null;
		}
		FileReader filerReader = null;
		FileWriter writer = null;
		BufferedReader br = null;
		File file = new File(path);
		if (file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						String dbName = files[i].getName();
						// 创建目标目录
						File resultFile = new File(targetNameFile.getAbsoluteFile() + File.separator + "process"
								+ File.separator + "creatTableBefore" + File.separator + dbName);
						isExit(resultFile);
						String errorPath = "";
						if (!path.contains("process")) {
							errorPath = files[i].getAbsolutePath() + File.separator + "StoreProcedure" + File.separator
									+ "error" + File.separator + "origin";
						} else {
							errorPath = files[i].getAbsolutePath();
						}
						File errorDir = new File(errorPath);
						File[] errorFiles = errorDir.listFiles();
						for (int j = 0; j < errorFiles.length; j++) {
							if (errorFiles[j].isDirectory()) {
								continue;
							}
							try {
								filerReader = new FileReader(errorFiles[j]);
								writer = new FileWriter(
										resultFile.getAbsolutePath() + File.separator + errorFiles[j].getName());
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
								result.setState(OPERATEFAIL1);
								result.setOperation("File write or read error(creat table before)");
								log.error("File write or read error:" + e.getMessage());
							} finally {
								if (br != null) {
									try {
										br.close();
									} catch (IOException e) {
										result.setState(OPERATEFAIL1);
										result.setOperation("BufferedReader read error(creat table before)");
										log.error("BufferedReader read error:" + e.getMessage());
									}
								}
								if (filerReader != null) {
									try {
										filerReader.close();
									} catch (IOException e) {
										result.setState(OPERATEFAIL1);
										result.setOperation("FileReader read error(creat table before)");
										log.error("FileReader read error:" + e.getMessage());
									}
								}
								if (writer != null) {
									try {
										writer.close();
									} catch (IOException e) {
										result.setState(OPERATEFAIL2);
										result.setOperation("FileWriter read error(creat table before)");
										log.error("FileWriter read error:" + e.getMessage());
									}
								}
							}
						}
					}
				}
			} else {
				result.setState(OPERATEFAIL1);
				result.setOperation("No files in the directory(creat table before)");
				log.error("No files in the directory");
			}
		} else {
			result.setState(OPERATEFAIL1);
			result.setOperation("The source file path cannot be non-existent or non-folder(creat table before)");
			log.error("The source file path cannot be non-existent or non-folder(creat table before)");
		}
		return targetNameFile.getAbsoluteFile() + File.separator + "process" + File.separator + "creatTableBefore";

	}

	/**
	 * 
	 * @return
	 */
	private String deleteTailReplaceFileHead(String sourceFile, String tagertFile, ClientResult result) {
		File targetNameFile = new File(tagertFile);
		if (!targetNameFile.exists()) {
			result.setState(OPERATEFAIL2);
			result.setOperation("Target path does not exist(deleteTailReplaceFileHead)");
			log.error("Target path does not exist(deleteTailReplaceFileHead)");
			return null;
		}
		FileReader filerReader = null;
		FileWriter writer = null;
		BufferedReader br = null;
		File file = new File(sourceFile);
		if (file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						String dbName = files[i].getName();
						File resultFile = new File(targetNameFile.getAbsoluteFile() + File.separator + "process"
								+ File.separator + "deleteTailReplaceFileHead" + File.separator + dbName);
						isExit(resultFile);
						String errorPath = "";
						if (!sourceFile.contains("process")) {
							errorPath = files[i].getAbsolutePath() + File.separator + "StoreProcedure" + File.separator
									+ "error" + File.separator + "origin";
						} else {
							errorPath = files[i].getAbsolutePath();
						}
						File errorDir = new File(errorPath);
						File[] errorFiles = errorDir.listFiles();
						for (int j = 0; j < errorFiles.length; j++) {
							if (errorFiles[j].isDirectory()) {
								continue;
							}
							try {
								filerReader = new FileReader(errorFiles[j]);
								writer = new FileWriter(
										resultFile.getAbsoluteFile() + File.separator + errorFiles[j].getName());
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
								result.setState(OPERATEFAIL1);
								result.setOperation("FileReader or FileReader is error(deleteTailReplaceFileHead)");
								log.error("FileReader or FileReader is error(deleteTailReplaceFileHead)");
							} finally {
								if (br != null) {
									try {
										br.close();
									} catch (IOException e) {
										result.setState(OPERATEFAIL1);
										result.setOperation("BufferedReader is error(deleteTailReplaceFileHead)");
										log.error("BufferedReaderis error(deleteTailReplaceFileHead)");
									}
								}
								if (filerReader != null) {
									try {
										filerReader.close();
									} catch (IOException e) {
										result.setState(OPERATEFAIL1);
										result.setOperation("FileReader is error(deleteTailReplaceFileHead)");
										log.error("FileReader is error(deleteTailReplaceFileHead)");
									}
								}
								if (writer != null) {
									try {
										writer.close();
									} catch (IOException e) {
										result.setState(OPERATEFAIL1);
										result.setOperation("FileWriter is error(deleteTailReplaceFileHead)");
										log.error("FileWriter is error(deleteTailReplaceFileHead)");
									}
								}
							}
						}
					}

				}
			} else {
				result.setState(OPERATEFAIL1);
				result.setOperation("No files in the directory(deleteTailReplaceFileHead)");
				log.error("No files in the directory(deleteTailReplaceFileHead)");
			}
		} else {
			result.setState(OPERATEFAIL1);
			result.setOperation("The source file path cannot be non-existent or non-folder(deleteTailReplaceFileHead)");
			log.error("The source file path cannot be non-existent or non-folder(deleteTailReplaceFileHead)");
		}
		return targetNameFile.getAbsoluteFile() + File.separator + "process" + File.separator
				+ "deleteTailReplaceFileHead";
	}

	/**
	 * Check if the file exists, not create
	 */
	private void isExit(File file) {
		if (file.exists()) {
			file.delete();
		}
		file.mkdirs();
	}

	/**
	 * 转换后fix文件 sourcePath 要求必须是DB文件夹的parent
	 */
	@Override
	public ClientResult handelConvertAfterFixed(String sourcePath, String targetPath, HashSet<String> types) {
		String tmpPath = sourcePath;
		ClientResult result = new ClientResult();
		File targetFile = new File(targetPath);
		if (!targetFile.exists()) {
			result.setState(OPERATEFAIL2);
			result.setOperation("Target path does not exist(handelConvertAfterFixed)");
			log.error("Target path does not exist(handelConvertAfterFixed)");
		}
		boolean fixFlag = false;

		// 1. create table After
		if (types.contains(RepariManFixType.TABLE)) {
			tmpPath = creatTableAfter(tmpPath, targetPath, result);
			if (result.getState() != 0) {
				return result;
			}
			fixFlag = true;
		}
		// 2. add header
		if (types.contains(RepariManFixType.COMMON_HEADER_TAIL)) {
			tmpPath = addHeader(tmpPath, targetPath, result);
			if (result.getState() != 0) {
				return result;
			}
			fixFlag = true;
		}
		// 3. s2ss todo
		S2ssUdf s2ss = new S2ssUdf();
		File fileS2ss = new File(tmpPath);
		if (fileS2ss.exists() && fileS2ss.isDirectory()) {
			File[] files = fileS2ss.listFiles();
			for (int i = 0; i < files.length; i++) {
				String dbName = files[i].getName();
				s2ss.handleS2ss(files[i].getAbsolutePath(), targetPath, dbName);
			}
		}

		if (fixFlag) {
			// copy结果文件
			try {
				File resultFile = new File(targetPath + File.separator + "result");
				isExit(resultFile);
				copyDir(tmpPath, targetPath + File.separator + "result");
				File target = new File(targetPath + File.separator + "s2ss");
				if (target.exists()) {
					copyDir(target.getAbsolutePath(), targetPath + File.separator + "result");
				}
			} catch (IOException e) {
				log.error("IOException when copy ", e);
			}
		}
		return result;
	}

	private String creatTableAfter(String sourcePath, String targetPath, ClientResult result) {
		File targetNameFile = new File(targetPath);
		if (!targetNameFile.exists()) {
			result.setState(OPERATEFAIL2);
			result.setOperation("Target path does not exist(creatTableAfter)");
			log.error("Target path does not exist(creatTableAfter)");
			return null;
		}
		// 创建目标目录
		File resultFile1 = new File(
				targetNameFile.getAbsoluteFile() + File.separator + "process" + File.separator + "creatTableAfter");
		isExit(resultFile1);

		FileReader filerReader = null;
		FileWriter writer = null;
		BufferedReader br = null;
		File file = new File(sourcePath);
		if (file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (int j = 0; j < files.length; j++) {
					if (files[j].isFile()) {
						continue;
					}
					File[] dbFiles = files[j].listFiles();
					String dbName = files[j].getName();
					File resultFile2 = new File(targetNameFile.getAbsoluteFile() + File.separator + "process"
							+ File.separator + "creatTableAfter" + File.separator + dbName);
					isExit(resultFile2);
					for (int i = 0; i < dbFiles.length; i++) {
						try {
							filerReader = new FileReader(dbFiles[i]);
							writer = new FileWriter(
									resultFile1 + File.separator + dbName + File.separator + dbFiles[i].getName());
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
							result.setState(OPERATEFAIL1);
							result.setOperation("FileReader and FileWriter error(creatTableAfter)");
							log.error("FileReader and FileWriter error(creatTableAfter) : " + e.getMessage());
						} finally {
							if (br != null) {
								try {
									br.close();
								} catch (IOException e) {
									result.setState(OPERATEFAIL1);
									result.setOperation("FileReader and FileWriter error(creatTableAfter)");
									log.error("FileReader and FileWriter error(creatTableAfter) : " + e.getMessage());
								}
							}
							if (filerReader != null) {
								try {
									filerReader.close();
								} catch (IOException e) {
									result.setState(OPERATEFAIL1);
									result.setOperation("FileReader close error(creatTableAfter)");
									log.error("FileReader close error(creatTableAfter) : " + e.getMessage());
								}
							}
							if (writer != null) {
								try {
									writer.close();
								} catch (IOException e) {
									result.setState(OPERATEFAIL1);
									result.setOperation("FileWriter close error(creatTableAfter)");
									log.error("FileWriter close error(creatTableAfter) : " + e.getMessage());
								}
							}
						}

					}
				}
			} else {
				result.setState(OPERATEFAIL1);
				result.setOperation("The source files not find(creatTableAfter)");
				log.error("The source files not find(creatTableAfter) : ");
			}
		} else {
			result.setState(OPERATEFAIL1);
			result.setOperation("The source file path cannot be non-existent or non-folder(creatTableAfter)");
			log.error("The source file path cannot be non-existent or non-folder(creatTableAfter) : ");
		}
		return resultFile1.getAbsolutePath();
	}

	/**
	 * 添加sqlserver头文件
	 * 
	 * @param sourcePath
	 * @param targetPath
	 * @param dbName
	 * @param result
	 * @return
	 */
	private String addHeader(String sourcePath, String targetPath, ClientResult result) {
		File targetNameFile = new File(targetPath);
		if (!targetNameFile.exists()) {
			result.setState(OPERATEFAIL2);
			result.setOperation("Target path does not exist(addHeader)");
			log.error("Target path does not exist(addHeader)");
			return null;
		}
		// 创建目标目录
		File resultFile1 = new File(
				targetNameFile.getAbsoluteFile() + File.separator + "process" + File.separator + "addHeader");
		isExit(resultFile1);

		FileReader filerReader = null;
		FileWriter writer = null;
		BufferedReader br = null;
		File file = new File(sourcePath);
		if (file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isFile()) {
						continue;
					}
					String dbName = files[i].getName();
					File resultFile2 = new File(targetNameFile.getAbsoluteFile() + File.separator + "process"
							+ File.separator + "addHeader" + File.separator + dbName);
					isExit(resultFile2);
					File[] sourceFiles = files[i].listFiles();
					for (int j = 0; j < sourceFiles.length; j++) {
						if (sourceFiles[j].isDirectory()) {
							continue;
						}
						try {
							filerReader = new FileReader(sourceFiles[j]);
							writer = new FileWriter(
									resultFile1 + File.separator + dbName + File.separator + sourceFiles[j].getName());
							br = new BufferedReader(filerReader);
							StringBuilder strBuild = new StringBuilder();
							String fengli = ".*((?i)create)\\s+((?i)Proc|(?i)PROCEDURE)(\\s+)(?<procName>[^ ^\\(]+).*";
							Pattern pafengli = Pattern.compile(fengli);

							while (true) {
								String line = br.readLine();
								if (line == null) {
									break;
								}
								if (line.matches(".*((?i)create)(\\s+)((?i)proc|(?i)procedure).*")) {
									Matcher matfengli = pafengli.matcher(line);
									if (matfengli.find()) {
										String procName = matfengli.group("procName");
										strBuild = new StringBuilder();
										strBuild.append("USE " + dbName + "\n");
										strBuild.append(RepairManConstant.HEADER);
										strBuild.append(RepairManConstant.deleteProc(procName));
									}

								}
								line = line + '\n';
								strBuild.append(line);
							}
							writer.write(strBuild.toString());
						} catch (Exception e) {
							result.setState(OPERATEFAIL1);
							result.setOperation("FileReader and FileWriter error(addHeader)");
							log.error("FileReader and FileWriter error(addHeader) : " + e.getMessage());
						} finally {
							if (br != null) {
								try {
									br.close();
								} catch (IOException e) {
									result.setState(OPERATEFAIL1);
									result.setOperation("FileReader and FileWriter error(addHeader)");
									log.error("FileReader and FileWriter error(addHeader) : " + e.getMessage());
								}
							}
							if (filerReader != null) {
								try {
									filerReader.close();
								} catch (IOException e) {
									result.setState(OPERATEFAIL1);
									result.setOperation("FileReader close error(addHeader)");
									log.error("FileReader close error(addHeader) : " + e.getMessage());
								}
							}
							if (writer != null) {
								try {
									writer.close();
								} catch (IOException e) {
									result.setState(OPERATEFAIL1);
									result.setOperation("FileWriter close error(addHeader)");
									log.error("FileWriter close error(addHeader) : " + e.getMessage());
								}
							}
						}
					}

				}
			} else {
				result.setState(OPERATEFAIL1);
				result.setOperation("The source files not find(addHeader)");
				log.error("The source files not find(addHeader) : ");
			}
		} else {
			result.setState(OPERATEFAIL1);
			result.setOperation("The source file path cannot be non-existent or non-folder(addHeader)");
			log.error("The source file path cannot be non-existent or non-folder(addHeader) : ");
		}
		return resultFile1.getAbsolutePath();
	}

	@Override
	public ClientResult handelSynchronizeFixed(String dirName) {

		return null;
	}

	/**
	 * 
	 * @param sourceDirPath
	 * @param targetDirPath
	 * @throws IOException
	 */
	private void copyDir(String sourceDirPath, String targetDirPath) throws IOException {
		// 获取源文件夹当前下的文件或目录
		File[] file = (new File(sourceDirPath)).listFiles();
		for (int i = 0; i < file.length; i++) {
			if (file[i].isDirectory()) {
				String dbName = file[i].getName();
				File[] resultFiles = file[i].listFiles();
				// 创建结果文件目录
				File resultFile = new File(targetDirPath + File.separator + dbName);
				isExit(resultFile);
				for (int j = 0; j < resultFiles.length; j++) {
					if (resultFiles[j].isFile()) {
						// 复制文件
						copyFileUsingFileStreams(resultFiles[j], new File(
								targetDirPath + File.separator + dbName + File.separator + resultFiles[j].getName()));
					}
				}
			} else {
				// 复制文件
				copyFileUsingFileStreams(file[i], new File(targetDirPath + File.separator + file[i].getName()));
			}
		}
	}

	private static void copyFileUsingFileStreams(File source, File dest) throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			input.close();
			output.close();
		}
	}

}