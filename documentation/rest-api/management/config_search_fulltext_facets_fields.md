Searchisko configuration - configuration of fields which are allowed for facet execution
==============================================================================

**configuration API id:** `search_fulltext_facets_fields`

This configuration enables configuration of [faceted search](http://en.wikipedia.org/wiki/Faceted_search) capabilities in Searchisko. 
Facet can be calculated on a document fields having correctly setup [mapping][mapping] and [analysis][analysis]. 
It is also important to note that calculation of facets can be system resources intensive (usually it requires more JVM Heap). 
Because of this the faceted search is limited to configured fields only.

Configuration of facet-enabled fields has the following format:

	"<facet_name>" : {
		"<facet_type>" : {
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

#### \<facet_name\>

The `<facet_name>` is used as value of `facet` URL parameter in [search API](http://docs.jbossorg.apiary.io/#searchapi). It also appears in the search response as respective key inside `facets` section.

#### \<facet_type\>

Currently only two facets types are supported:

- `terms`: refers to [Terms Facet][terms facet] type. Recommended to use with `string` or number-like [field type][field type]s. It works best for fields that do not have a high cardinality.
- `date_histogram`: refers to [Date Histogram Facet][date histogram facet] type. It is designed to be used for [date type][date type]. I.e. `{ "type" : "date" }`.

#### \<document_field_name\>

Specify actual **document field** that is used. **This field is mandatory.**

#### \<optional_settings\>

Additional settings. Currently only `size` setting is supported for `terms` facet type (see the example above).

#### \<_filtered\>

This setting that can be used **only with `terms` facet type**. This settings is experimental and subject to change in the future.


_Note: faceted search capabilities of Elasticsearch are very powerful and flexible. Searchisko exposes only small set of the full Elasticsearch API functionality. However, going forward this may change and facets API will be exposed in much opened way (yet it will still need to be controlled). See full [Elasticsearch Facets documentation][elasticsearch facets documentation] for details._

  [mapping]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/mapping.html
  [analysis]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/analysis.html
  [field type]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/mapping-types.html
  [date type]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/mapping-core-types.html#date
  [terms facet]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-facets-terms-facet.html
  [date histogram facet]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-facets-date-histogram-facet.html
  [elasticsearch facets documentation]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-facets.html
