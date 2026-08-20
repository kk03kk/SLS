/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.nntp;

import java.io.PrintStream;
import java.util.ArrayList;
import org.apache.commons.net.nntp.Threadable;

public class Article
implements Threadable {
    private long articleNumber = -1L;
    private String subject;
    private String date;
    private String articleId;
    private String simplifiedSubject;
    private String from;
    private ArrayList<String> references;
    private boolean isReply = false;
    public Article kid;
    public Article next;

    public void addReference(String msgId) {
        if (msgId == null || msgId.length() == 0) {
            return;
        }
        if (this.references == null) {
            this.references = new ArrayList();
        }
        this.isReply = true;
        for (String s : msgId.split(" ")) {
            this.references.add(s);
        }
    }

    public String[] getReferences() {
        if (this.references == null) {
            return new String[0];
        }
        return this.references.toArray(new String[this.references.size()]);
    }

    private void simplifySubject() {
        int start = 0;
        String subject = this.getSubject();
        int len = subject.length();
        boolean done = false;
        while (!done) {
            int end;
            done = true;
            while (start < len && subject.charAt(start) == ' ') {
                ++start;
            }
            if (!(start >= len - 2 || subject.charAt(start) != 'r' && subject.charAt(start) != 'R' || subject.charAt(start + 1) != 'e' && subject.charAt(start + 1) != 'E')) {
                if (subject.charAt(start + 2) == ':') {
                    start += 3;
                    done = false;
                } else if (start < len - 2 && (subject.charAt(start + 2) == '[' || subject.charAt(start + 2) == '(')) {
                    int i;
                    for (i = start + 3; i < len && subject.charAt(i) >= '0' && subject.charAt(i) <= '9'; ++i) {
                    }
                    if (i < len - 1 && (subject.charAt(i) == ']' || subject.charAt(i) == ')') && subject.charAt(i + 1) == ':') {
                        start = i + 2;
                        done = false;
                    }
                }
            }
            if ("(no subject)".equals(this.simplifiedSubject)) {
                this.simplifiedSubject = "";
            }
            for (end = len; end > start && subject.charAt(end - 1) < ' '; --end) {
            }
            if (start == 0 && end == len) {
                this.simplifiedSubject = subject;
                continue;
            }
            this.simplifiedSubject = subject.substring(start, end);
        }
    }

    public static void printThread(Article article) {
        Article.printThread(article, 0, System.out);
    }

    public static void printThread(Article article, PrintStream ps) {
        Article.printThread(article, 0, ps);
    }

    public static void printThread(Article article, int depth) {
        Article.printThread(article, depth, System.out);
    }

    public static void printThread(Article article, int depth, PrintStream ps) {
        for (int i = 0; i < depth; ++i) {
            ps.print("==>");
        }
        ps.println(article.getSubject() + "\t" + article.getFrom() + "\t" + article.getArticleId());
        if (article.kid != null) {
            Article.printThread(article.kid, depth + 1);
        }
        if (article.next != null) {
            Article.printThread(article.next, depth);
        }
    }

    public String getArticleId() {
        return this.articleId;
    }

    public long getArticleNumberLong() {
        return this.articleNumber;
    }

    public String getDate() {
        return this.date;
    }

    public String getFrom() {
        return this.from;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setArticleId(String string) {
        this.articleId = string;
    }

    public void setArticleNumber(long l) {
        this.articleNumber = l;
    }

    public void setDate(String string) {
        this.date = string;
    }

    public void setFrom(String string) {
        this.from = string;
    }

    public void setSubject(String string) {
        this.subject = string;
    }

    @Override
    public boolean isDummy() {
        return this.articleNumber == -1L;
    }

    @Override
    public String messageThreadId() {
        return this.articleId;
    }

    @Override
    public String[] messageThreadReferences() {
        return this.getReferences();
    }

    @Override
    public String simplifiedSubject() {
        if (this.simplifiedSubject == null) {
            this.simplifySubject();
        }
        return this.simplifiedSubject;
    }

    @Override
    public boolean subjectIsReply() {
        return this.isReply;
    }

    @Override
    public void setChild(Threadable child) {
        this.kid = (Article)child;
        this.flushSubjectCache();
    }

    private void flushSubjectCache() {
        this.simplifiedSubject = null;
    }

    @Override
    public void setNext(Threadable next) {
        this.next = (Article)next;
        this.flushSubjectCache();
    }

    @Override
    public Threadable makeDummy() {
        return new Article();
    }

    public String toString() {
        return this.articleNumber + " " + this.articleId + " " + this.subject;
    }

    @Deprecated
    public int getArticleNumber() {
        return (int)this.articleNumber;
    }

    @Deprecated
    public void setArticleNumber(int a) {
        this.articleNumber = a;
    }

    @Deprecated
    public void addHeaderField(String name, String val) {
    }
}

