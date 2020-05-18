package io.mbrc.newsfetch.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryParameters {

    public enum SortOrder {
        ASC("ASC"),
        DESC("DESC");

        public String repr;

        SortOrder(String repr) {
            this.repr = repr;
        }

        @Override
        public String toString() {
            return this.repr;
        }
    }

    public enum SortBy {
        SCORE("_score"),
        DISCOVER_DATE("discoverDate"),
        READ_TIME("metadata.readTime.seconds");

        public String repr;

        SortBy(String repr) {
            this.repr = repr;
        }

        @Override
        public String toString() {
            return this.repr;
        }
    }

    private String query;
    private SortBy sortBy;
    private SortOrder sortOrder;
    private int limit;
}
