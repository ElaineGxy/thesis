public void initProcess(){
    while(SubmitedPapaer.exists()){ //存在为评阅的试卷
        List<Integer>paperIDList=submitPaper.getUnmarkedPaperID();
        for(int paperID:paperIDList){
			StringBuilder resultStr=new StringBuilder();
            List<Integer> programIDList=submitPaper.getProgramIDList(paperID);
			for(int programID:programIDList){
				String program=submitPaper.getProgramByID(programID,resultStr);
				int lagType=Commons.getLag(program);
				if(lagType==2){//java类型
					JProcessjp=new JProcess(programID,resultStr); //处理程序源代码，并将处理的结果写入到数据库中
					jp.start();
				}else(lagType==0){//C/C++类型
					CProcesscp=new CProcess(programID,resultStr);
					cp.start();
				}
			}
			
            
        }
    }
}


public void run(){
	Program currentProgram=submitPaper(this.programID);
	String stdProgram=currentProgram.getStdProg();
	String stdOut=currentProgram.getStdOut();
	int stdScore=currentProgram.getScore();
	boolean compiledResult=compile(currentProgram);
	float score=0.0;
	if(compiledResult){//编译通过
		long before=Calendar.getInstance().getTimeInMills();//获取开始运行的时间
		String out=execute(currentProgram);
		long after=Calendar.getInstance().getTimeInMills();//运行结束时间
		long realTime=after-before;
		if(realTime>=this.timeThreshold){
			float rightRatio=calSimilarity(currentProgram); //计算提交的源代码与标准代码的相似度
			score=rightRatio*stdScore;
		}else{
			score=equalRatio(out,stdOut)*stdScore;//将输出与标准答案进行比对
			if(out==null || score==0.0){//程序运行出错
				float rightRatio=calSimilarity(currentProgram); //计算提交的源代码与标准代码的相似度
				score=rightRatio*stdScore;
			}
		}
	}else{
		float rightRatio=calSimilarity(currentProgram);
		score=rightRatio*stdScore*0.8; //测试数据完全没通过，那么会减去20%的分数，然后在此基础上通过相似度比例计算最终得分。
	}
	this.resultStr.append('<item><id>'+this.programID+'</id><type>1</type><score>'+score+'</score><item>');//按照指定的结果记录结果
}


import java.io.BufferedReader;  
import java.io.FileReader;  
import java.io.FileWriter;  
import java.io.IOException;  
import java.util.ArrayList;  
import java.util.Enumeration;  
import java.util.HashMap;  
import java.util.Hashtable;  
import java.util.LinkedList;  
import java.util.List;  
import java.util.Map;  
public class Lexer {  
    /* 记录行号 */  
    public static int line = 1;  
    /* 记录列号 */  
    public static int rows = 0;  
    /* 下一个读入字符 */  
    char peek = ' ';  
    Hashtable<String, String> words = new Hashtable<String, String>();  
    /* erros 表 */  
    private Hashtable<String, Error> errors = new Hashtable<String, Error>();  
    /* token序列 */  
    private List<String> tokens = new LinkedList<String>();  
    /* 源地址 */  
    private String SOURCE_PATH = "";  
    /* 符号表地址 */  
    private String SYMBOL_TABLE = "";  
    /* Tokens表 */  
    private String TOKEN_TABLE = "";  
    /* 错误信息表 */  
    private String ERROR_TABLE = "";  
    /* 读取文件变量 */  
    BufferedReader reader = null;  
    /* 保存当前是否读取到了文件的结尾 */  
    private Boolean isEnd = false;  
  
    /* 是否读取到文件的结尾 */  
    public Boolean getReaderState() {  
        return this.isEnd;  
    }  
    /* 
     * 构造函数中将关键字和类型添加到hashtable words中 
     */  
    public Lexer(String sourcepath, String symobolpath, String tokenpath, String errortable) {  
        SOURCE_PATH = sourcepath;  
        SYMBOL_TABLE = symobolpath;  
        TOKEN_TABLE = tokenpath;  
        ERROR_TABLE = errortable;  
        /* 初始化读取文件变量 */  
        try {  
            reader = new BufferedReader(new FileReader(SOURCE_PATH));  
        } catch (IOException e) {  
            System.out.print(e);  
        }  
        /* 关键字 */  
        reserve(Word.and, "运算符");  
        reserve(Word.or, "运算符");  
        reserve(Word.eq, "运算符");  
        reserve(Word.ne, "运算符");  
        reserve(Word.e, "运算符");  
        reserve(Word.g, "运算符");  
        reserve(Word.l, "运算符");  
        reserve(Word.n, "运算符");  
        reserve(Word.True, "关键字");  
        reserve(Word.False, "关键字");  
        reserve(Word.Int, "关键字");  
        reserve(Word.Double, "关键字");  
        reserve(Word.Float, "关键字");  
        reserve(Word.Char, "关键字");  
        reserve(Word.Boolean, "关键字");  
        reserve(Word.String, "关键字");  
        reserve(Word.Private, "关键字");  
        reserve(Word.Protected, "关键字");  
    }  
    void reserve(String key, String vaule) {  
        words.put(key, vaule);  
    }  
    /* 读文件字符 */  
    public void readch() throws IOException {  
        /* 这里应该是使用的是 */  
        peek = (char) reader.read();  
        if ((int) peek == 0xffff) {  
            this.isEnd = true;  
        }  
        // peek = (char)System.in.read();  
    }  
    /* 读文件下一个字符 */  
    public Boolean readch(char ch) throws IOException {  
        readch();  
        if (this.peek != ch) {  
            return false;  
        }  
        this.peek = ' ';  
        return true;  
    }  
    /* 程序词法分析 */  
    public String scan() throws IOException {  
        /* 消除空白 */  
        for (;; readch()) {  
            if (peek == ' ' || peek == '\t') {  
                rows++;  
                continue;  
            } else if (peek == '\n') {  
                line = line + 1;  
                rows = 0;  
            } else  
                break;  
        }  
        rows++;  
        /* 下面开始分割关键字，标识符等信息 */  
        switch (peek) {  
        /* 对于 ==, >=, <=, !=的区分使用状态机实现 */  
        case '=':  
            if (readch('=')) {  
                tokens.add("==");  
                return Word.eq;  
            } else {  
                tokens.add("=");  
                return Word.e;  
            }  
        case '>':  
            if (readch('=')) {  
                tokens.add(">=");  
                return Word.ge;  
            } else {  
                tokens.add(">");  
                return Word.g;  
            }  
        case '<':  
            if (readch('=')) {  
                tokens.add("<=");  
                return Word.le;  
            } else {  
                tokens.add("<");  
                return Word.l;  
            }  
        case '!':  
            if (readch('=')) {  
                tokens.add("!=");  
                return Word.ne;  
            } else {  
  
                tokens.add("!");  
                return Word.n;  
            }  
        case '~':  
        case '@':  
        case '$':  
        case '%':  
        case '^':  
        case '&':  
            /* 
             * if(peek == ' ' || peek == '\t'){ tokens.add(peek+""); 
             * errors.put(peek+"", new Error(line,rows,"单独的符号错误")); return 
             * peek+""; } 
             */  
            String vs = "";  
            do {  
                vs += peek;  
                readch();  
                if(peek == ' ' || peek == '\t'){  
                    break;  
                }  
            } while (true);  
            errors.put(vs, new Error(line, rows, "符号命名错误"));  
            return vs;  
        }  
        /* 
         * 下面是对数字的识别，根据文法的规定的话，这里的 数字只要是能够识别整数就行. 
         */  
        if (Character.isDigit(peek)) {  
            int value = 0;  
            String vs = "";  
            do {  
                value = 10 * value + Character.digit(peek, 10);  
                vs += value;  
                readch();  
            } while (Character.isDigit(peek));  
            if ((peek >= 'a' && peek <= 'z') || (peek >= 'A' && peek <= 'Z')) {  
                vs = vs + peek;  
                checkTag(vs);  
                tokens.add(vs);  
                readch();  
                return vs;  
            } else {  
                tokens.add(value + "");  
                words.put(value + "", "数字");  
                return value + "";  
            }  
        }  
        /* 
         * 关键字或者是标识符的识别 
         */  
        if (Character.isLetter(peek)) {  
            StringBuffer sb = new StringBuffer();  
  
            /* 首先得到整个的一个分割 */  
            do {  
                sb.append(peek);  
                readch();  
            } while (Character.isLetterOrDigit(peek));  
            /* 判断是关键字还是标识符 */  
            String s = sb.toString();  
            String t = words.get(s);  
            /* t 为关键字 */  
            /* 如果是关键字或者是类型的话，w不应该是空的 */  
            if (t != null) {  
                tokens.add(s);  
                return s; /* 说明是关键字 或者是类型名 */  
            }  
            /* 否则就是一个标识符id */  
            if (checkTag(s)) {  
                tokens.add(s);  
                words.put(s, "标识符");  
                return s;  
            } else {  
                return s;  
            }  
        }  
        /* peek中的任意字符都被认为是词法单元返回 */  
        String ss = "" + (char) peek;  
        // table.put(tok, "Token or Seprator");  
        if ((int) peek != 0xffff)  
            tokens.add(ss);  
        peek = ' ';  
        return ss;  
    }  
    /* 标识符规则 */  
    public boolean checkTag(String str) {  
        char beg = str.charAt(0);  
        /* 不能以符号开头 */  
        switch (beg) {  
        /* 不能以数字开头 */  
        case '0':  
        case '1':  
        case '2':  
        case '3':  
        case '4':  
        case '5':  
        case '6':  
        case '7':  
        case '8':  
        case '9':  
            errors.put(str, new Error(line, rows, "不能用数字开头"));  
            return false;  
        case '~':  
        case '!':  
        case '@':  
        case '$':  
        case '%':  
        case '^':  
        case '&':  
        case '*':  
        case '(':  
        case ')':  
            errors.put(str, new Error(line, rows, "不能用符号开头"));  
            return false;  
        default:  
            return true;  
        }  
    }  
    /* 保存error */  
    public void saveErrorTable() throws IOException {  
        FileWriter writer = new FileWriter(ERROR_TABLE);  
        writer.write("[符号]          [错误类型]          [错误位置]\n");  
        writer.write("\r\n");  
        Enumeration<String> e = errors.keys();  
        while (e.hasMoreElements()) {  
            String key = e.nextElement();  
            Error err = errors.get(key);  
            writer.write(key + "\t\t\t" + err.getMsg() + "\t\t\t" + err.getHh() + "行" + err.getLh() + "列" + "\r\n");  
        }  
        writer.flush();  
    }  
    /* 保存Tokens */  
    public void saveTokens() throws IOException {  
        FileWriter writer = new FileWriter(TOKEN_TABLE);  
        writer.write("[符号]  \n");  
        writer.write("\r\n");  
  
        for (int i = 0; i < tokens.size(); ++i) {  
            String tok = tokens.get(i);  
            /* 写入文件 */  
            writer.write(tok + "\r\n");  
        }  
        writer.flush();  
    }  
    /* 保存存储在table中的 */  
    public void saveSymbolsTable() throws IOException {  
        FileWriter writer = new FileWriter(SYMBOL_TABLE);  
        writer.write("[符号]          [符号类型信息]\n");  
        writer.write("\r\n");  
        for (int i = 0; i < tokens.size(); ++i) {  
            String tok = tokens.get(i);  
            String desc = words.get(tok);  
            if (desc != null) {  
                /* 写入文件 */  
                writer.write(tok + "\t\t\t" + desc + "\r\n");  
            }  
        }  
        writer.flush();  
    }  
}  

public void run(){
	int questionSize=this.questionReqList.size();
	产生 0~qestionSize的随机排列
	for(int i=0;i<questionSize;i++){
		QuestionReq req=this.questionReqList.get(i);
		Question question=QuestionDAO.getQuestionByReq(req);
		将question加入试卷中
	}
}

