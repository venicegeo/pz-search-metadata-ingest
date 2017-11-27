/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package piazza.commons.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.logger.Severity;
import util.OsValidator;
import util.PiazzaLogger;

//@Component
public class NativeElasticsearchTemplate {
	private static final Logger LOG = LoggerFactory.getLogger(NativeElasticsearchTemplate.class);

	@Value("${vcap.services.pz-elasticsearch.credentials.hostname}")
	private String elasticHostname;
	@Value("${elasticsearch.transportClientPort}")
	private Integer elasticPort;
	
	@Autowired
	private PiazzaLogger logger;
	@Autowired
	private Client client;
	@Autowired
	OsValidator osValidator;
	
	private ObjectMapper mapper;

	public NativeElasticsearchTemplate() {
		// Expected for Component instantiation
	}

	public NativeElasticsearchTemplate(Client client, ObjectMapper mapper) {
		this.client = client;
		this.mapper = mapper;
	}

	public boolean createIndex(String indexName) {
		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);
		CreateIndexResponse response = createIndexRequestBuilder.execute().actionGet();

		return response.isAcknowledged();
	}

	public boolean createIndexWithMapping(String indexName, String type, String mapping) {

		String appCurrentDirectory;
		try {
			appCurrentDirectory = new java.io.File(".").getCanonicalPath();
			printDirectoryRecursive( appCurrentDirectory );
		} catch (IOException e) {
			LOG.error("Could not create Index with Mapping", e);
		}

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);
		if (mapping != null) {
			createIndexRequestBuilder.addMapping(type, mapping);
		}
		CreateIndexResponse response = createIndexRequestBuilder.execute().actionGet();

		return response.isAcknowledged();
	}

	/**
	 * Creates the initial index from a script file located in resources/db folder of the app.
	 * 
	 * @param indexName the index to be passed on to script file
	 * @param type the type of index to be created
	 * @return boolean for success/fail.
	 * @throws IOException
	 */
	public boolean createIndexWithMappingFromShellScript(String indexName, String aliasName, String indexDataType) throws IOException {
		try {
			// Build path to script file
			String filename = (osValidator.isWindows()) ? ("initial_pzmetadataIndex.bat") : ("initial_pzmetadataIndex.sh");
			String appCurrentDirectory = new java.io.File(".").getCanonicalPath();
			String scriptPath = String.format("%s%s%s%s%s%s%s%s%s%s%s", appCurrentDirectory, File.separator, "BOOT-INF", File.separator, "classes", File.separator, "db", File.separator, "000-Create-Initial_Indexes", File.separator, filename);
			
			// Only for local initialization for windows developers
			if (osValidator.isWindows()) {
				scriptPath = String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s", appCurrentDirectory, File.separator, "src",
						File.separator, "main", File.separator, "resources", File.separator, "db", File.separator,
						"000-Create-Initial_Indexes", File.separator, filename);
			} else {
				// Change script execute permissions
				File file = new File(scriptPath);
				boolean wasCreated = file.createNewFile();
				if (!wasCreated) {
					logger.log(String.format("Could not create file %s because it already exists. Overwriting.", scriptPath), Severity.INFORMATIONAL);
				}
				Set<PosixFilePermission> perms = new HashSet<>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				perms.add(PosixFilePermission.OWNER_EXECUTE);
				perms.add(PosixFilePermission.GROUP_EXECUTE);
				Files.setPosixFilePermissions(file.toPath(), perms);
			}

			// Initialize urls
			String baseUrl = String.format("%s:%s", elasticHostname, elasticPort);
			String indexCreationUrl = String.format("%s/%s/", baseUrl, indexName);
			String aliasCreationUrl = String.format("%s/_aliases/", baseUrl);

			String scriptParameters = String.format("%n ScriptPath: %s %n IndexDataType: %s %n IndexUrl: %s %n IndexName: %s %n AliasName: %s %n AliasUrl: %s", scriptPath, indexDataType, indexCreationUrl, indexName, aliasName, aliasCreationUrl);
			LOG.debug("Running piazza metadata index creation with following parameters: {}", scriptParameters);
			
			// Run the shell script with parameters
			ProcessBuilder pb = new ProcessBuilder(scriptPath, indexDataType, indexCreationUrl, indexName, aliasName, aliasCreationUrl);
			Process p = pb.start();
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			String error = String.format("Unable to run index creation scripts for index %s. %s", indexName,
					e.getMessage());
			logger.log(error, Severity.ERROR);
			LOG.error(error, e);
			return false;
		}
		return true;
	}
	
    public void printDirectoryRecursive( String path ) {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) {
        	return;
        }

        for ( File f : list ) {
            if ( f.isDirectory() ) {
            	printDirectoryRecursive( f.getAbsolutePath() );
            	LOG.info("Dir: {}", f.getAbsoluteFile());            	
            }
            else {
            	LOG.info("File: {}", f.getAbsoluteFile());
            }
        }
    }

	public boolean indexExists(String indexName) {
		return client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
	}

	public <T extends ESModel> boolean index(String indexName, String type, T o) {
		boolean created = false;

		try {
			String source = mapper.writeValueAsString(o);
			IndexResponse response = client.prepareIndex(indexName, type, o.getId()).setSource(source, XContentType.JSON).get();
			created = checkIfIndexExists(indexName);

			if (created) {
				o.setId(response.getId());
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			logger.log(e.getMessage(), Severity.ERROR);
		}

		return created;
	}

	/**
	 * Checking index existence 
	 * 
	 * @param index
	 * @return boolean
	 */
	private boolean checkIfIndexExists(String index) {
		IndexMetaData indexMetaData = client.admin().cluster().state(Requests.clusterStateRequest()).actionGet().getState().getMetaData()
				.index(index);

		return (indexMetaData != null);
	}
	
	/*
	 * CSS presently unused; remove for test code coverage % public <T extends
	 * ESModel> void bulkIndex( String indexName, String type, Collection<T>
	 * objects ) { try { BulkRequestBuilder bulkRequest = client.prepareBulk();
	 * 
	 * for (T object : objects) { String source = mapper.writeValueAsString(
	 * object); bulkRequest.add( client.prepareIndex( indexName, type,
	 * object.getId()).setSource( source)); }
	 * 
	 * BulkResponse response = bulkRequest.execute().get();
	 * 
	 * // not so friendly way of allowing collections // unfortunately
	 * collection does not implement get(index) if (objects instanceof List) {
	 * List<T> l = (List<T>) objects;
	 * 
	 * for (BulkItemResponse item : response.getItems()) { if (!item.isFailed())
	 * { l.get( item.getItemId()).setId( item.getId()); } } }
	 * 
	 * } catch (Exception e) { logger.log(e.getMessage(), PiazzaLogger.ERROR); }
	 * }
	 */

	/*
	 * CSS presently unused; remove for test code coverage % public <T extends
	 * ESModel> List<T> queryForList( SearchRequestBuilder searchQuery, Class<T>
	 * clazz ) {
	 * 
	 * SearchResponse response = searchQuery.setSize(
	 * Integer.MAX_VALUE).execute().actionGet();
	 * 
	 * List<T> results = new ArrayList<T>( response.getHits().getHits().length);
	 * 
	 * for (int i = 0; i < response.getHits().getHits().length; ++i) { try {
	 * SearchHit hit = response.getHits().getHits()[i]; T result =
	 * mapper.readValue( hit.getSourceAsString(), clazz); result.setId(
	 * hit.getId()); results.add( result); } catch (Exception e) {
	 * logger.log(e.getMessage(), PiazzaLogger.ERROR); } }
	 * 
	 * return results; }
	 * 
	 * public <T extends ESModel> List<T> queryForPage( SearchRequestBuilder
	 * searchQuery, int pageNumber, int pageSize, Class<T> clazz ) {
	 * 
	 * SearchResponse response = searchQuery .setFrom( pageNumber * pageSize)
	 * .setSize( pageSize) .execute() .actionGet();
	 * 
	 * List<T> results = new ArrayList<T>( response.getHits().getHits().length);
	 * 
	 * for (int i = 0; i < response.getHits().getHits().length; ++i) { try {
	 * SearchHit hit = response.getHits().getHits()[i]; T result =
	 * mapper.readValue( hit.getSourceAsString(), clazz); result.setId(
	 * hit.getId()); results.add( result); } catch (Exception e) {
	 * logger.log(e.getMessage(), PiazzaLogger.ERROR); } }
	 * 
	 * results.removeAll( Arrays.asList( "", null));
	 * 
	 * return results; }
	 */

	public <T extends ESModel> T findOne(String index, String type, String id, Class<T> clazz) {

		T result = null;
		try {
			GetResponse response = client.prepareGet(index, type, id).execute().get();
			if (response.isExists()) {
				result = mapper.readValue(response.getSourceAsBytes(), clazz);
				result.setId(response.getId());
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			logger.log(e.getMessage(), Severity.ERROR);
		}

		return result;
	}

	/*
	 * 2.x changes query builders CSS 5/14/16 public SearchRequestBuilder
	 * NativeSearchQueryBuilder() { return new SearchRequestBuilder(
	 * this.client); }
	 */
	public SearchRequestBuilder NativeSearchQueryBuilder() {
		return new SearchRequestBuilder(this.client, null);
	}

	public <T extends ESModel> boolean delete(String index, String type, T instance) {
		try {
			DeleteResponse response = client.prepareDelete(index, type, instance.getId()).execute().get();
			return true;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			logger.log(e.getMessage(), Severity.ERROR);
		}
		return false;
	}

	public <T extends ESModel> int delete(String index, String type, Collection<T> objects) {
		int result = 0;

		try {

			BulkRequestBuilder bulkRequest = client.prepareBulk();

			for (T object : objects) {
				bulkRequest.add(client.prepareDelete(index, type, object.getId()));
			}

			BulkResponse response = bulkRequest.execute().get();

			if (response.hasFailures()) {
				result = countNumResponseFailures(response.getItems());
			} 
			else {
				result = objects.size();
			}

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			logger.log(e.getMessage(), Severity.ERROR);
		}

		return result;
	}
	
	private int countNumResponseFailures(BulkItemResponse[] items) {
		int numResponseFailures = 0;
		
		for (BulkItemResponse item : items) {
			if (!item.isFailed())
				++numResponseFailures;
		}

		return numResponseFailures;
	}
	
	public boolean refresh(String index) {
		boolean success = false;

		try {
			RefreshResponse result = client.admin().indices().prepareRefresh(index).execute().get();
			success = (result.getShardFailures().length == 0);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			logger.log(e.getMessage(), Severity.ERROR);
		}

		return success;
	}
}
