Searchisko configuration - configuration of fields which are allowed for aggregation execution
==============================================================================

**[configuration API](http://docs.jbossorg.apiary.io/#managementapiconfiguration) id:** `search_fulltext_aggregations_fields`

This configuration enables configuration of data aggregation (i.e. generalized [Faceted search](http://en.wikipedia.org/wiki/Faceted_search))
capabilities in Searchisko. 
Aggregations can be calculated on a document fields having correctly setup [mapping][mapping] and [analysis][analysis]. 
It is also important to note that calculation of aggregations can be system resources intensive (usually it requires more JVM Heap). 
Because of this the aggregation execution is limited to configured fields only.

Configuration of aggregation-enabled fields has the following format:

	"<aggregation_name>" : {
		"<aggregation_type>" : {
			"field" : "<document_field_name>",
			<optional_settings>
		},
		<_filtered>
	}

Example:

	"activity_dates_histogram" : {
		"date_histogram" : {
			"field" : "sys_activity_dates"
		}
	},
	"tag_cloud" : {
		"terms" : {
			"field" : "sys_tags",
			"size" : 50
		}
	},
	"top_contributors" : {
		"terms" : {
			"field" : "sys_contributors",
			"size" : 100
		},
		"_filtered" : { "size" : 30 }
	}

#### \<aggregation_name\>

The `<aggregation_name>` is used as value of `agg` URL parameter in [search API](http://docs.jbossorg.apiary.io/#searchapi).
It also appears in the search response as respective key inside `aggregations` section.

#### \<aggregation_type\>

Currently only two aggregation types are supported:

- `terms`: refers to [Terms Aggregation][terms agg] type. Recommended to use with `string` or number-like [field type][field type]s. It works best for fields that do not have a high cardinality.
- `date_histogram`: refers to [Date Histogram Aggregation][date histogram agg] type. It is designed to be used for [date type][date type]. I.e. `{ "type" : "date" }`.

#### \<document_field_name\>

Specify actual **document field** that is used. **This field is mandatory.**

#### \<optional_settings\>

Additional settings. Currently only `size` setting is supported for `terms` aggregation type (see the example above).

#### \<_filtered\>

This setting that can be used **only with `terms` aggregation type**. This settings is experimental and subject to change in the future.


_Note: Aggregation capabilities of Elasticsearch are very powerful and flexible. Searchisko exposes only small set
of the full Elasticsearch API functionality in its Search API. However, once #123 is implemented it will be possible
to use any kind of aggregations.
See full [Elasticsearch Aggregations documentation][elasticsearch aggs documentation] for details._

  [mapping]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/mapping.html
  [analysis]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/analysis.html
  [field type]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/mapping-types.html
  [date type]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/mapping-core-types.html#date
  [terms agg]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-aggregations-bucket-terms-aggregation.html
  [date histogram agg]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-aggregations-bucket-datehistogram-aggregation.html
  [elasticsearch aggs documentation]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-aggregations.html
