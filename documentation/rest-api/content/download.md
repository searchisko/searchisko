Download logs
=============

**sys\_type = "download"**

Data stored in this sys_type is there to describe downloads of files. It's gathered from Apache access logs and initially used for storing downloads.jboss.org data.

## Data structure

### Standard system fields
<table border="1">
    <thead>
        <th>Field</th>
        <th>Example value</th>
        <th width="63%">Description</th>
    </thead>
    <tbody>
        <tr><td>sys_content_id</td><td>jbossorg_download_logs-2455:27772610</td><td>A unique value created by prefixing Splunk unique id with sys_content_type value of 'jbossorg_download_logs'</td></tr>
        <tr><td>sys_created</td><td>2015-04-22T19:23:50.000Z</td><td>Timestamp when the download request happened.</td></tr>
        <tr><td>sys_project</td><td>jbosstools</td><td>Preprocessed from the download URI, project id.</td></tr>
        <tr><td>sys_project_name</td><td>JBoss Tools</td><td>Based on sys_project a full project name is stored in this field.</td></tr>
        <tr><td>sys_tags</td><td>is_source_patch</td><td>Additional information for clasifying the download e.g. whether it was a binary, hash file or a patch.</td></tr>
        <tr><td>sys_title</td><td>/jbosstools/static/releases/jbosstools-4.2.3.Final-updatesite-core/plugins/org.jboss.ide.eclipse.as.jmx.integration_3.0.3.Final-v20150325-0035-B129.jar.pack.gz</td><td>URI path extracted from the download</td></tr>
        <tr><td>sys_url_view</td><td>http://downloads.jboss.org/jbosstools/static/releases/jbosstools-4.2.3.Final-updatesite-core/plugins/org.jboss.tools.as.runtimes.integration_3.0.3.Final-v20150325-0035-B129.jar.pack.gz</td><td>URL to the download that was logged.</td></tr>
    </tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here. Description may be found in general documentation for ["Searchisko Content object"](dcp_content_object.md).

### Custom fields
<table border="1">
    <thead>
        <th>Field</th>
        <th>Example value</th>
        <th width="63%">Description</th>
    </thead>
    <tbody>
        <tr><td>bytes</td><td>1000000</td><td>Number of bytes trasnfered while serving this download.</td></tr>
        <tr><td>client_ip</td><td>127.0.0.1</td><td>IP number of the computer requesting the download resource.</td></tr>
        <tr><td>cookie</td><td></td><td>Cookie value if any was linked to this particular download request in the log.</td></tr>
        <tr><td>country</td><td>CH</td><td>Two letter code identifying the country from which the URL was requested.</td></tr>
        <tr><td>file</td><td>org.jboss.tools.as.runtimes.integration_3.0.3.Final-v20150325-0035-B129.jar.pack.gz</td><td>File name extracted from the URI.</td></tr>
        <tr><td>file_ext</td><td>gz</td><td>File extension.</td></tr>
        <tr><td>path</td><td>/jbosstools/static/releases/jbosstools-4.2.3.Final-updatesite-core/plugins/org.jboss.ide.eclipse.as.jmx.integration_3.0.3.Final-v20150325-0035-B129.jar.pack.gz</td><td>Path of the download, copied also to sys_title.</td></tr>
        <tr><td>product</td><td>false</td><td>A boolean value depending on sys_project field saying whether the download is related to a product.</td></tr>
        <tr><td>referer</td><td>http://www.jboss.org</td><td>URL of the page which referred the downloaded file.</td></tr>
        <tr><td>release_no</td><td>3.0.3</td><td>If possible to extract here is a release number of the particular download file.</td></tr>
        <tr><td>tags</td><td>is_source_patch</td><td>Currently it's exactly same content as in sys_tags field but we keep a copy in case sys_tags field changes its purpose.</td></tr>
        <tr><td>user_agent</td><td>Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET CLR 1.0.3705; .NET CLR 1.1.4322)</td><td>A complete description of web agent making the download request.</td></tr>
    </tbody>
</table>