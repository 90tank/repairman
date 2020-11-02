package com.aia.repairman.util;

import lombok.Data;

@Data
public class ClientResult {

	private int state;
	private String operation;
	private String errorMessage;
}
