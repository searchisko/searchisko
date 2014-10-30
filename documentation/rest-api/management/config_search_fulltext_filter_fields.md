Searchisko configuration - configuration of fields which are allowed for filters
==============================================================================

**[configuration API](http://docs.jbossorg.apiary.io/#managementapiconfiguration) id:** `search_fulltext_filter_fields`

Configuration of Elasticsearch [filters](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/query-dsl-filters.html) in Searchisko.

The following filter types are supported:

### Terms filter

Uses [Terms filter].

	"<filter_name>" : {
		"terms" : {
			"<field_name>" : "{{}}",
			<optional_settings>
		},
		"_suppress" : [<filter_name>, <filter_name>, ...],
		"_lowercase" : <boolean>
	}

Only one `terms` filter can be defined per `<field_name>`.

_Note: The `terms` filter can be [aliased with `in`](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/query-dsl-terms-filter.html#query-dsl-terms-filter).
This alias is supported._

#### \<_suppress\>

Allows to specify that this filter suppress listed filters. For example if filter B is configured to suppress filter A
then if both filters A and B are provided by the client then filter A is ignored and only the filter B is used.

#### \<_lowercase\>

By default terms used for [Terms filter] match are not analyzed. However, because we feed the field values in this
filter directly from URL request parameters we allow to specify if those values should be lowercased before passing
into the filter. This can be useful if the analysis for the field in the document uses
[lowercase token filter](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/analysis-lowercase-tokenfilter.html).
A standard Java [String#toLowerCase(Locale.ENGLISH)](http://docs.oracle.com/javase/7/docs/api/java/lang/String.html)
function is used (no ICU magic).

 [Terms filter]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/query-dsl-terms-filter.html

### Range filter

Uses [Range filter](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/query-dsl-range-filter.html).

	"<filter_name>" : {
		"range" : {
			"<field_name>" : {
				gte: "{{}}",
				lte: "{{}}"
			}
			<optional_settings>
		},
		"_suppress" : [<filter_name>, <filter_name>, ...],
		"_processor" : "<className>"
	}

Range filter in Elasticsearch allows for both upper and lower bounds to be set. However, filters in Searchisko map
to URL parameters which means we can pass only a single value per filter thus only one option (`gte` or `lte`) is
allowed per range filter configuration. This limitation fits well to schema where user sets bounds by different URL
parameters (for example `&to=2012-12-31&from=2010-01-01`). However, we can map both filters to the same document filed
which will result into a single Elasticsearch range filter to be instantiated with both upper and lower bounds set
accordingly.

Options `lt` and `gt` are not supported for now.

For example:

	"from" : {
		"range" : {
			"sys_activity_dates" : {
				"gte" : "{{}}"
			}
		}
	},
	"to" : {
		"range" : {
			"sys_activity_dates" : {
				"lte" : "{{}}"
			}
		}
	}

Note both `from` and `to` filters apply to the **same field name** `sys_activity_dates`. This has the following consequences:

- If only one of `from` or `to` URL parameters are set by the user then appropriate range filter is instantiated accordingly using only `gte` or `lte` option respectively.
- If, however, both URL parameters are present then again a single filter is instantiated but both upper and lower bounds are setup.

#### \<_suppress\>

Allows to specify that this filter suppress listed filters. For example if filter B is configured to suppress filter A
then if both filters A and B are provided by the client then filter A is ignored and only the filter B is used.

#### \<_processor\>

Maps to class name implementing [`org.searchisko.api.model.ParsableIntervalConfig`] interface. The class also has to be
Java enum. If specified then this class is used to transform user input value to output value for the filter.

The class also has to implement method `ParsableIntervalConfig parseRequestParameterValue(String requestVal)` which is currently
not forced by the `org.searchisko.api.model.ParsableIntervalConfig`. This is an flaw in current design and will be improved
in the future version.

Supported processor types:

- `org.searchisko.api.util.SearchUtils.PastIntervalValue`
  - input: `day`, `week`, `month`, `quarter` or `year`
  - output: a value in millis corresponding to `now - ${input}`

## General Comments

Be careful when using `<_suppress>` because there is not guarantied any particular order in which filters are processed.
As a result complex rules can lead to unpredictable results. Generally speaking, it should be used exceptionally and
with great care.

The `<optional_settings>` can contain other Elasticsearch specific settings for particular filter type.
This allows to pass the following settings: `_name`, `_cache`, `_cache_key` (or `_cacheKey`).

## Comprehensive Filter Configuration Example

Put everything together:

	{
		"project" : {
			"terms" : {
				"sys_project" : "{{}}"
			}
		},
		"tag" : {
			"terms" : {
				"sys_tags" : "{{}}"
			},
			"_lowercase" : true
		},
		"activity_date_from" : {
			"range" : {
				"sys_activity_dates" : {
					"gte" : "{{}}"
				}
			}
		},
		"activity_date_to" : {
			"range" : {
				"sys_activity_dates" : {
					"lte" : "{{}}"
				}
			}
		},
		"activity_date_interval" : {
			"range" : {
				"sys_activity_dates" : {
					"gte" : "{{}}"
				}
			},
			"_suppress" : ["activity_date_from", "activity_date_to"],
			"_processor" : "org.searchisko.api.model.PastIntervalValue"
		}
	}

Using the configuration above the following URL query parts can be processed:

`&project=hibernate&project=weld&project=wildfly&activity_date_from=2012-01-01&activity_date_interval=week`

will result into:

- `terms filter` with values `["hibernate","weld","wildfly"]`
- `range filter` with value `gte: $now - week`
- URL parameter `activity_date_from` will be ignored
