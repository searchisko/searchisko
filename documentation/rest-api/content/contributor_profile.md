Contributor Profile
===================

**sys\_type = "contributor_profile"**

This data type stored in Searchisko contains public information about Contributors.
Data for DCP come from jboss.org user profile managed in Jive - [JBoss Developer](https://developer.jboss.org).


## Data structure

Data structure is inspired by [OpenSocial specification - Person object](http://opensocial-resources.googlecode.com/svn/spec/2.0.1/Social-Data.xml#Person) specification.

Example of contribprofile data structure:

	{
		"sys_content_provider": "jbossorg",
		"sys_type": "contributor_profile",
		"sys_content_type": "jbossorg_contributor_profile",

		"id": "jbossorg_contribprofile-lkrzyzanek",
		"sys_id": "jbossorg_contribprofile-lkrzyzanek",

		"sys_contributors": ["Libor Krzyzanek <fake@fake.com>"],

		"name": {
			"givenName": "Libor",
			"familyName": "Krzyzanek",
			"formatted": "Libor Krzyzanek"
		},
		"displayName": "Libor Krzyzanek",
		"sys_title": "Libor Krzyzanek",

		"tags": [ "jboss.org", "sbs" ],
		"sys_tags": [ "jboss.org", "sbs" ],

		"published": "2009-01-13T05:00:00.000+0000",
		"sys_created": "2009-01-13T05:00:00.000+0000",

		"updated": "2013-12-03T13:06:37.972+0000",
		"sys_updated": "2014-01-20T13:06:37.972+0000",

		"profileUrl": "https://developer.jboss.org/people/lkrzyzanek",
		"sys_url_view": "https://developer.jboss.org/people/lkrzyzanek",

		"timeZone": "Europe/Berlin",

		"thumbnailUrl": "https://developer.jboss.org/api/core/v3/people/6100/avatar",


		"aboutMe": "BIO",
		"sys_description": "BIO",

		"accounts": [
			{
				"domain": "github.com",
				"username": "lkrzyzanek-ght"
			},
			{
				"domain": "twitter.com",
				"username": "lkrzyzanek-twitter"
			},
			{
				"domain": "facebook.com",
				"username": "lkrzyzanek-facebook"
			}
		]
	}
	
Description of data fields for the issue record:
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
	<tr><td>sys_type</td><td>Always `contributor_profile`</td></tr>
	<tr><td>sys_contributors</td><td>Contributor Code mapped to the profile</td></tr>
	<tr><td>updated</td><td>Timestamp when profile has been updated in Profile Provider (e.g. developer.jboss.org)</td></tr>
	<tr><td>sys_updated</td><td>Timestamp when ES document has been updated</td></tr>
	<tr><td>sys_url_view, profileUrl</td><td>URL to Profile page from Profile Provider</td></tr>
	<tr><td>timeZone</td><td>Timezone defined by Profile Provider for the profile</td></tr>
	<tr><td>thumbnailUrl</td><td>URL containing Profile's Avatar </td></tr>
	<tr><td>sys_description, aboutMe</td><td>BIO</td></tr>
	<tr><td>accounts</td><td>Array of Accounts in other systems (eg, Twitter, Facebook etc.) related to this profile.</td></tr>
</tbody>
</table>
