package com.yuansong.tools.svrv3;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.MessageFormat;

public class SvrV3Hepler {

private SvrV3Hepler(){}
	
	private static byte chSplit = 9;
	private static byte chEdge = 0;
	
	private static int timeout = 10000;

	public static int getTimeout() {
		return timeout;
	}

	public static void setTimeout(int timeout) {
		SvrV3Hepler.timeout = timeout;
	}

	/**
	 * 获取SVRV3服务器的SQL配置（Socket通讯）
	 * @param server 服务器IP地址
	 * @param port 端口
	 * @param appType 类型
	 * @param timeout 超时时间（毫秒）
	 * @return
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public static SQLConfig getSQLConfig(String server, int port, int appType, int timeout) throws UnknownHostException, IOException {
		byte[] result = getSvrV3SQLConfig(server, port, appType, timeout);
		return convertConfig(result);
	}
	
	public static SQLConfig getSQLConfig(String server, int port, int appType) throws UnknownHostException, IOException {
		return getSQLConfig(server, port, appType, timeout);
	}
	
	public static SQLConfig getSQLConfig(int port, int appType, int timeout) throws UnknownHostException, IOException {
		return getSQLConfig("127.0.0.1", port, appType, timeout);
	}
	
	public static SQLConfig getSQLConfig(int port, int appType) throws UnknownHostException, IOException {
		return getSQLConfig("127.0.0.1", port, appType, timeout);
	}
	
	private static SQLConfig convertConfig(byte[] data) throws UnsupportedEncodingException {
		String str = new String(data, "GBK");
		str = str.replaceAll(new String(new byte[] {chEdge}), "");
		String[] list = str.split(new String(new byte[] {chSplit}));

		if(list.length < 3) {
			throw new RuntimeException("convert sql config error, limit 2 act " + String.valueOf(list.length));
		}
		if(!list[0].equals("RESCONNECT")) {
			throw new RuntimeException("convert sql config error, not export format");
		}
		if(list[1].equals("0")) {
			throw new RuntimeException("error: " + list[2]);
		} else if (!list[1].equals("1")) {
			throw new RuntimeException("convert sql config error, second val exp 0 or 1 act " + list[1]);
		}
		if(list.length < 4) {
			throw new RuntimeException("convert sql config error, exp limit 4 act " + String.valueOf(list.length));
		}
		SQLConfig config = new SQLConfig();
		String server = list[2];
		String[] serverList = server.split(",");
		if(serverList.length > 1) {
			config.setServer(serverList[0]);
			config.setPort(Integer.valueOf(serverList[1]));
		} else {
			config.setServer(serverList[0]);
			config.setPort(null);
		}
		config.setUsername(list[3]);
		if(list.length >= 5) {
			config.setPassword(list[4]);			
		} else {
			config.setPassword("");
		}
		return config;
	}
	
	private static byte[] getSvrV3SQLConfig(String server, int port, int appType, int timeout) throws UnknownHostException, IOException  {
		
		byte[] msg = getSocketMsg(appType);
		
		Socket socket = null;
		OutputStream os = null;
		DataOutputStream dos = null;
		InputStream is = null;
		byte[] result = null;
		
		try {
			socket = new Socket(server, port);
			socket.setSoTimeout(timeout);
					
			os = socket.getOutputStream();
			dos = new DataOutputStream(os);
	
			dos.write(msg);
			dos.flush();
			
			is = socket.getInputStream();
					
			result = readInputStream(is);
	
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {}
			}
			if(dos != null) {
				try {
					dos.close();
				} catch (IOException e) {}
			}
			if(os != null) {
				try {
					os.close();
				} catch (IOException e) {}
			}
			if(socket != null) {
				try {
					socket.close();
				} catch (IOException e) {}
			}
		}
	    return result;
	}
	
	private static  byte[] readInputStream(InputStream dis) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = -1;
		byte[] result = null;
		boolean flag = false;
		try {
			do {
				len = dis.read(buffer);
				if(!flag && buffer[0] == chEdge) flag = true;
				if(len > 0 && flag) {
					bos.write(buffer, 0, len);
				}
			}while(len > 0 && len == buffer.length && buffer[len-1] != chEdge);
			result = bos.toByteArray();	
		} finally {
			try {
				bos.close();
			} catch (IOException e) {}			
		}
		return result;
    }
	
	private static byte[] getSocketMsg(int appType) {
//		//CONNECT*AppType*ClientType*ComputerIP*ComputeName
//		String socketMsg = MessageFormat.format("{0}CONNECT{1}{2}{3}{4}{5}{6}{7}{8}{9}", 
//				chEdge, chSplit, "83", chSplit, clientType, chSplit, computerIp, chSplit, computerName, chEdge);
		
		
		String computerIp = "127.0.0.1";
		String computerName = "tool";
		
		byte[] msg = new byte[0];
		msg = byteMerger(msg, chEdge);
		msg = byteMerger(msg, "CONNECT".getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, String.valueOf(appType).getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, String.valueOf(0).getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, computerIp.getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, computerName.getBytes());
		msg = byteMerger(msg, chEdge);
		
		return msg;
	}
		
	private static byte[] byteMerger(byte[] bt1, byte[] bt2){  
        byte[] bt3 = new byte[bt1.length+bt2.length];  
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);  
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);  
        return bt3;  
    } 
	
	private static byte[] byteMerger(byte[] bt, byte b) {
		byte[] btr = new byte[bt.length + 1];
		System.arraycopy(bt, 0, btr, 0, bt.length);
		btr[btr.length - 1] = b;
		return btr;
	}
	
//	private static byte[] byteMerger(byte b, byte[] bt) {
//		byte[] btr = new byte[bt.length + 1];
//		btr[0] = b;
//		System.arraycopy(bt, 0, btr, 1, bt.length);
//		return btr;
//	}
	
	/**
	 * 获取账套查询语句
	 * @param appType 系统类型
	 * @param flag 是否仅正式账套
	 * @return
	 */
	public static String getAccSql(int appType, boolean flag) {
		String accTable = MessageFormat.format("zlaccount{0}", String.valueOf(appType));
		StringBuilder sb = new StringBuilder();
		sb.append(""
				+ "select accname "
				+ "from ").append(accTable).append(" ");
		sb.append("where accisdeleted = 0 ");
			if(flag) {
				sb.append("and acctype = 1 ");
			}
			sb.append("order by accname asc");
		return sb.toString();
	}
	
}
