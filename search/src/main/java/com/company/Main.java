package com.company;
import com.github.houbb.word.checker.util.EnWordCheckers;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.sql.*;
import java.util.List;


public class Main {

    public static String Soundex(String word){
        char[] chars=new char[]{'0','1','2','3','0','1','2','0','0','2','2','4','5','5',
                '0','1','2','6','2','3','0','1','0','2','0','2'};
        int MAXCHARS=4;
        char[] wordChars=null;
        char[] result=new char[4];
        if(word==null||(wordChars=word.trim().toCharArray()).length==0){
            return null;
        }
        //下标
        int index=-1;
        //当前位移
        int cur=0;
        //Soundex字符串已填充字符数
        int fill=0;
        while(cur<wordChars.length&&fill<MAXCHARS){
            char c=wordChars[cur++];
            if((c>='A'&&c<='Z')){
                index=c-'A';
                if(fill==0){
                    c+='a'-'A';
                }
            }else if((c>='a'&&c<='z')){
                index=c-'a';
            }else{
                index=-1;
            }
            if(index!=-1){
                if(fill==0){
                    result[fill++]=c;
                }else{
                    char curChar=chars[index];
                    if(curChar!='0'&&(fill==1||curChar!=result[fill-1])){
                        result[fill++]=curChar;
                    }
                }
            }
        }
        if(fill==0){
            return null;
        }
        for(int i=MAXCHARS-fill;i>0;i--){
            result[MAXCHARS-i]='0';
        }
        return new String(result);
    }

    public static String Correct(String word){
        HashMap<String, String > soundex_table = new HashMap<String, String>();
        String key = Soundex(word);
        try{
            BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\DeGraw\\Desktop\\search\\src\\main\\resources\\dictionary.txt"));
            String s = null;
            while((s = br.readLine())!=null){
                soundex_table.put(Soundex(s),s);
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        if(soundex_table.get(key)==null){
            return word;
        }
        else{
            return soundex_table.get(key);
        }
    }

    public static String input_process(String input){
        //author: a AND content: dramatically AND id: 3
        //author: a AND content: SPELL(helllo) AND id: 3
        //author: a AND content: SPELL(helllo) AND SOUNDEX(conthideriball) AND id: 3
        //NOT author: a AND content: dramaticaally AND brilliant
        //NOT author: a AND here AND content: dramatically AND brilliant
        //NOT body: show AND here AND NOT content: dramatically AND brilliant OR summary: am
        //content: dramatically AND brilliant AND body: show
        //content: dramatically AND brilliant AND body: show AND ss
        //content: dramatically AND brilliant AND body: show AND s*s
        String[] tmp = input.split("[ ]");
        Vector<String> middle= new Vector<>(Arrays.asList(tmp));
        // 后续添加合法性查询
        if(middle.size()<2){
            return "ERROR";
        }
        else{
            int i=0;
            while (i<middle.size()){
                if (middle.get(i).equals("NOT")){
                    i+=3;
                }else{
                    middle.insertElementAt("IS",i);
                    i+=3;
                }
                if(i<middle.size()){
                    if(middle.get(i).equals("AND") || middle.get(i).equals("OR") ){
                        i+=1;
                        if(middle.get(i).charAt(middle.get(i).length() - 1)!=':'){
                            middle.insertElementAt(middle.get(i-3),i);
                            middle.insertElementAt(middle.get(i-4),i);
                            i+=4;
                        }
                    }
                    else{
                        return "ERROE";
                    }
                }
            }
        }

        for(int i=1;i<middle.size();i+=4){
            middle.set(i, middle.get(i).substring(0, middle.get(i).length() - 1));
        }

        //Soundex and Spell-correction
        boolean flag=false;
        Vector<String> change= new Vector<String>();
        for(int i=2;i<middle.size();i+=4){
            if(middle.get(i).indexOf("SOUNDEX")!=-1){
                flag=true;
                String word = middle.get(i).substring(8, middle.get(i).length() - 2);
                String correct = Correct(word);
                change.add(correct);
                middle.set(i,correct);
            }
            if(middle.get(i).indexOf("SPELL")!=-1){
                flag=true;
                String word = middle.get(i).substring(6, middle.get(i).length() - 2);
                String correct = EnWordCheckers.correct(word);
                change.add(correct);
                middle.set(i,correct);
            }
        }

        for(int i=2;i<middle.size();i+=4){
            String s=middle.get(i).replace("*", "%");
            s = "%"+s+"%";
            middle.set(i,s);
        }

//        for(String s:middle)
//            System.out.println(s);

        String sql="SELECT * FROM `reddit`.`1` WHERE ";
        //SELECT * FROM `reddit`.`1` WHERE `author` LIKE '%a%' AND `body` LIKE '%a%' AND `content_len` LIKE '2%3' AND NOT(`author` = 'NightlyReaper')
        if(flag){
            String correct = "";
            for(String s:change){
                correct += (s + " ");
            }
            System.out.println("Do you mean by \""+correct+"\"?");
        }
        for(int i=0;i<middle.size();i+=4){
            if(i==middle.size()-3){
                if(middle.get(i).equals("IS")){
                    sql+=("`"+middle.get(i+1)+"` LIKE '"+middle.get(i+2)+"' ");
                }
                else{
                    sql+=(middle.get(i)+"(`"+middle.get(i+1)+"` = '"+middle.get(i+2)+"') ");
                }
            }
            else{
                if(middle.get(i).equals("IS")){
                    sql+=("`"+middle.get(i+1)+"` LIKE '"+middle.get(i+2)+"' "+middle.get(i+3)+" ");
                }
                else{
                    sql+=(middle.get(i)+"(`"+middle.get(i+1)+"` = '"+middle.get(i+2)+"') "+middle.get(i+3)+" ");
                }
            }
        }
//        System.out.println(sql);
        return sql+" LIMIT 0,100";
    }

    public static void search(String sql){
        ResultSet rs = null;
        Statement stmt = null;
        Connection conn = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = (Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/reddit?useUnicode=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&useSSL=FALSE" ,"root","root");
            stmt = conn.createStatement();

            rs = stmt.executeQuery(sql);
            if (rs.next()==false){
                System.out.println("Found no results.");
            }
            while (rs.next()) {
                String author = rs.getString("author");
                //int age = rs.getInt("summary_len");

                System.out.println(author);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Illegal queries!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Illegal queries!");
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
                if (stmt != null) {
                    stmt.close();
                    stmt = null;
                }
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Illegal queries!");
            }
        }
    }

    public static void main(String[] args) {

        while(true){
            System.out.println("Please input your search...");
            Scanner sc=new Scanner(System.in);
            String input=sc.nextLine();

            String sql=input_process(input);
            if(sql.equals("ERROR")){
                System.out.println("Illegal queries!");
                continue;
            }
            search(sql);
        }
    }
}