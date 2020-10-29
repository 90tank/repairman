package com.aia.repairman.sybase.comment;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PushbackReader;

import static sun.nio.ch.IOStatus.EOF;

public class CommentConvertUtil {

    public CommnetConvertState state = CommnetConvertState.NULL_STATE;
    public int pairState = 0;

    public static void main(String[] args) throws IOException {
        FileReader filerReader = new FileReader("D:\\test\\nest_comment_sql.sql");

        FileWriter writer = new FileWriter("D:\\test\\fixed_nest_comment_sql.sql");
        int pushBackLimit = 5;
        PushbackReader reader = new PushbackReader(filerReader,pushBackLimit);
        CommentConvertUtil convertUtil = new CommentConvertUtil();
        convertUtil.blockComment2SingleLineComment(reader, writer);
    }

    // /* --
    // */ --
    // -- --
    /**
     * Block comment --> Single line comment
     *
     * @param fileReader
     * @param fileWriter
     * @throws IOException
     */
    public void blockComment2SingleLineComment(PushbackReader  fileReader, FileWriter fileWriter) throws IOException {
        while (state != CommnetConvertState.END_STATE)
        {
            switch (state)
            {
                case NULL_STATE:
                    nullState_of_B2S(fileReader, fileWriter);
                    break;
                case BLOCK_COMMENT_STATE:
                    blockState_of_B2S(fileReader, fileWriter);
                    break;
                case SINGLE_LINE_COMMMENT_STATE:
                    singleCommentState_of_B2S(fileReader, fileWriter);
                    break;
                case STRING_STATE:
                    stringConvert(fileReader, fileWriter);
            }
        }
        fileReader.close();
        fileWriter.close();
    }


    /**
     * 无状态处理逻辑
     * @param fileReader
     * @param fileWriter
     */
    public void nullState_of_B2S(PushbackReader fileReader, FileWriter fileWriter) throws IOException {
        int ch = fileReader.read();

        switch (ch)
        {
            case '/':
                int next_ch = fileReader.read();
                if (next_ch == '*') {
                    fileWriter.write('-'); //instead by single comment symbol
                    fileWriter.write('-');
                    fileWriter.write('/');
                    fileWriter.write('*');

                    this.pairState += 1; // 当遇到 /* 时候 parCode 计数器+1 ，在遇到*/后 -1， 为0则表示成对
                    state = CommnetConvertState.BLOCK_COMMENT_STATE;
                } else { // 其他字符， 直接写入文件， 状态保持前一个状态
                    fileWriter.write(ch);
                    fileWriter.write(next_ch);
                }
                break;
            case '-':
                int next_ch1 = fileReader.read();
                if (next_ch1 == '-') {
                    fileWriter.write('-');
                    fileWriter.write('-');

                    state = CommnetConvertState.SINGLE_LINE_COMMMENT_STATE;
                } else {
                    fileWriter.write(ch);
                    fileWriter.write(next_ch1); // eg: return -1
                }
                break;

            case EOF: // 文件读取结束

                state = CommnetConvertState.END_STATE;
                break;
            default:
                fileWriter.write(ch);
                break;
        }
    }




    /**
     * blockState 杠星 状态处理逻辑
     * B2S : block comment to single comment
     * @param fileReader
     * @param fileWriter
     */
    void blockState_of_B2S(PushbackReader fileReader, FileWriter fileWriter) throws IOException {
        int ch = fileReader.read();
        switch (ch) {
            case '/':
                int next_char = fileReader.read();
                if (next_char == '*') {
                    fileWriter.write('-'); //instead by single comment symbol
                    fileWriter.write('-');
                    fileWriter.write('/');
                    fileWriter.write('*');
                    this.pairState += 1; // 当遇到 /* 时候 parCode 计数器+1 ，在遇到*/后 -1， 为0表示 成对儿
                } else {
                    fileWriter.write(ch);
                    fileReader.unread(next_char); // 指针回退
                }
                break;
            case '*':
                int next_ch = fileReader.read();
                if (next_ch == '/') {
                    this.pairState -= 1;

                    fileWriter.write('*'); // 此处是将*/ 替换成 -- （实际也可以不替换）
                    fileWriter.write('/');
                    int ch_1 = fileReader.read();
                    int ch_2 = fileReader.read();


                    // 下一字符非换行或文件结尾
                    // CR LF
                    // 13 10 注意读取 和回退的方向 ！！！
                    if (('\r' != ch_1)&&(('\n' != ch_2))  && (EOF != ch_1) ) {  // */ 后面不是换行 文件也没有结束 ，输出文件写入换行符号
                        // 遇到*/ 发现已经成对
                        if (this.pairState == 0) { // 若成对则换行，换行之后状态变为未知
                            //输出回车是因为要是在遇到“*/”，说明注释已经结束了，后面的数据
                            //不再是注释的一部分，而此时本行数据已经被“--”修饰为注释内容，要是不换行，“*/”
                            //后面的数据也会被当做是注释的一部分,即使“*/”后面仍为注释，那么也最好输出回车，
                            //因为这是两块相互独立的注释，不换行，就会被认为是一条注释
                            fileWriter.write('\n');
                            state = CommnetConvertState.NULL_STATE;

                            fileReader.unread(ch_2);
                            fileReader.unread(ch_1);

                            System.out.println( "state : " +this.pairState );
                            System.out.println("unread 写入换行符号 \\n");

                            System.out.println(ch_2);
                            System.out.println(ch_1);
                        } else {
                            // 非成对 后面的内容无需换行

                            fileReader.unread(ch_2);
                            fileReader.unread(ch_1);

                            System.out.println("*/ 不成对 unread");
                            System.out.println(ch_2);
                            System.out.println(ch_1);
                        }


                    // 下4字符是换行或文件结尾 （表示后面已经没有内容 只有换行符或者文件结束符 ，则直接写入）
                    } else {
                        fileWriter.write('\n'); // */ 后面若是换行 或 EOF ，则直接写入

                        System.out.println("print \\n after pair */ ");
                        if(this.pairState == 0) {
                            // 若成对 则下次进入无状态
                            state = CommnetConvertState.NULL_STATE;
                        } else {
                            // 若不成对 则下次仍然进入本状态 但是换行后 要添加 --  重要
                            fileWriter.write('-');
                            fileWriter.write('-');
                        }
                    }
                } else if (next_ch == '*') {  // ** 把星放回去，重要
                    fileWriter.write(ch); // 将上个*写入，这个*暂不写入 作为下次判断依据
                    //在此需要回退一个自己的指针，是因为碰到**之后可能接下来的字符时是/，
                    // 那么会和它的上一个字符*组成一个*/，成为C语言注释的结束标志，
                    // 所以需要回退一个字符，判断此时连续读取的两个字符会不会是*/
                    fileReader.unread(next_ch);
                } else {
                    fileWriter.write(ch);
                    fileWriter.write(next_ch);
                }
                break;
            case EOF:
//                fileWriter.write(ch);
                state = CommnetConvertState.END_STATE;
                break;

            case '\n':
                fileWriter.write(ch);
                fileWriter.write('-'); // 块注释中遇到换行 则写入换行并单行注释下一行
                fileWriter.write('-');
                break;
            default:
                fileWriter.write(ch);
                break;
        }
    }


    /**
     * singleCommentState --
     * 单行注释处理逻辑
     */
    void singleCommentState_of_B2S(PushbackReader fileReader, FileWriter fileWriter) throws IOException {
        int ch = fileReader.read();
        switch (ch) {
            // 换行符替换
            case '\r':
                int the_next_char = fileReader.read();
                if (the_next_char == '\n') {
                    fileWriter.write('\n');
                }
                state = CommnetConvertState.NULL_STATE;
                break;
            case '\n':
                fileWriter.write(ch);
                state = CommnetConvertState.NULL_STATE; // 行结束 (linux or windows)
                break;
            case EOF:
//                fileWriter.write(ch);
                state = CommnetConvertState.END_STATE; // 文件结束
                break;
            default:
                fileWriter.write(ch); // 双杠后面的 都直接写入，只要不遇到换行或文件结束 ，状态保持不变
                break;
        }
    }

    /**
     * 字符串转换
     * @param fileReader
     * @param fileWriter
     * @throws IOException
     */
    void stringConvert(PushbackReader fileReader, FileWriter fileWriter) throws IOException {
        int ch = fileReader.read();
        switch (ch) {
            case '"':
                fileWriter.write(ch);
                state = CommnetConvertState.NULL_STATE;
                break;
            case EOF:
//                fileWriter.write(ch);
                state = CommnetConvertState.END_STATE;
                break;
            // 换行符替换
            case '\r':
                int the_next_char = fileReader.read();
                if (the_next_char == '\n') {
                    fileWriter.write('\n');
                }
                state = CommnetConvertState.NULL_STATE;
                break;
            default:
                fileWriter.write(ch);
                break;
        }
    }

}



