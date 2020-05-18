package io.mbrc.newsfetch.client;

import com.google.gson.reflect.TypeToken;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

@Data
public class NewsType {

    public static Type newsCollectionType = new TypeToken<List<NewsType>>() {
    }.getType();

    @Data
    public class Element {
        public String type;
        public Boolean primary;
        public String url;
        public Object width;
        public Object height;
        public Object title;
        public Object alternative;
    }

    @Data
    public class Website {
        public String name;
        public String hostName;
        public String domainName;
        public String iconURL;
        public String countryName;
        public String countryCode;
        public Object region;
    }

    @Data
    public class MetaData {

        @Data
        public class ReadTime {
            public String type;
            public Float seconds;
        }

        @Data
        public class Category {
            public String type;
            public String country;
            public String region;
            public String category;
            public Object countryCode;
        }

        public ReadTime readTime;
        public Category category;
    }

    public String id;
    public Date publishDate;
    public Date discoverDate;
    public String title;
    public String language;
    public String text;
    public String structuredText;
    public String url;
    public Element[] elements;
    public Website website;
    public MetaData metadata;
    public Float score;

}
