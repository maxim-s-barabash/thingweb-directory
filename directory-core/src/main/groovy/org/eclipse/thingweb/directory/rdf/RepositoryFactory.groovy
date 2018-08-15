package org.eclipse.thingweb.directory.rdf

import groovy.transform.Immutable
import groovy.util.logging.Log
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryException
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore

/**
 * 
 * .
 *
 * @author Victor Charpenay
 * @creation 14.08.2018
 *
 */
@Log
class RepositoryFactory {
	
	/**
	 * Boolean value indicating that the default in-memory should not be used (default: false).
	 * If no proper configuration is found (either as factory parameters or as environment variables),
	 * an exception is thrown.
	 */
	static final REMOTE_SPARQL_ENDPOINT_ONLY = 'THINGWEB_REMOTE_SPARQL_ENDPOINT_ONLY'

	/**
	 * URL of a remote SPARQL endpoint to use for persistence. SPARQL Update must be allowed.
	 * Note: if this parameter is not provided, the remote only parameter is ignored.
	 */
	static final SPARQL_QUERY_ENDPOINT_PROPERTY = 'THINGWEB_SPARQL_QUERY_ENDPOINT'

	/**
	 * URL of the update endpoint, if different from the SPARQL query endpoint.
	 * Defaults to query endpoint, otherwise.
	 */
	static final SPARQL_UPDATE_ENDPOINT_PROPERTY = 'THINGWEB_SPARQL_UPDATE_ENDPOINT'

	/**
	 * Username to use to connect to the provided SPARQL endpoint (HTTP basic authentication).
	 */
	static final SPARQL_USERNAME_PROPERTY = 'THINGWEB_SPARQL_USERNAME'

	/**
	 * Password to use to connect to the provided SPARQL endpoint (HTTP basic authentication).
	 */
	static final SPARQL_PASSWORD_PROPERTY = 'THINGWEB_SPARQL_PASSWORD'
	
	//@Immutable
	private static class RepositoryInit {
		Boolean remoteOnly
		String queryEndpoint
		String updateEndpoint
		String username
		String password
	}

	/**
	 * Single reference to an in-memory RDF store, shared across all RDF manager instances.
	 */
	private static Repository sharedRepo

	/**
	 * Performs the following operations until one succeeds (in this order):
	 * 
	 * <ol>
	 *   <li>Tries to connect to a remote SPARQL endpoint based on factory parameters</li>
	 *   <li>Tries to connect to a remote SPARQL endpoint based on environment variables</li>
	 *   <li>Creates a transient in-memory RDF store (or throws an exception)</li>
	 * </ol>
	 * 
	 * @param params factory parameters
	 * @return an RDF store object
	 */
	static Repository get(Map params = [:]) {
		RepositoryInit init

		if (params[SPARQL_QUERY_ENDPOINT_PROPERTY]) {
			init = fromParams(params)
			log.info('Connecting to SPARQL endpoint from factory parameters...')
		} else if (System.getenv(SPARQL_QUERY_ENDPOINT_PROPERTY)) {
			init = fromEnv()
			log.info('Connecting to SPARQL endpoint from environment variables...')
		}
		
		if (init) {
			SPARQLRepository repo = new SPARQLRepository(init.queryEndpoint, init.queryEndpoint)
			repo.initialize()
			
			if (init.username && init.password) {
				repo.setUsernameAndPassword(init.username, init.password)
			} else if (init.username || init.password) {
				log.warning('Provided SPARQL endpoint credentials are incomplete')
			}

			try {
				// probe to test SPARQL endpoint availability
				// TODO request SPARQL service description and check for sd:UnionDefaultGraph
				repo.getConnection().isEmpty()
				return repo
			} catch (RepositoryException e) {
				log.warning('SPARQL endpoint cannot be reached')
				
				if (init.remoteOnly) {
					throw new RuntimeException('No suitable SPARQL endpoint configuration found', e)
				}
			}
		}
		
		if (!sharedRepo) {
			sharedRepo = new SailRepository(new MemoryStore())
			sharedRepo.initialize()
			log.fine('Initializing transient in-memory RDF store (unique per JVM)...')
		}
		
		return sharedRepo
	}
	
	/**
	 * Builds an initialization object from factory parameters.
	 * 
	 * @param params factory parameter map
	 * @return a SPARQL endpoint initialization object
	 */
	private static RepositoryInit fromParams(Map params) {
		def remote = params[REMOTE_SPARQL_ENDPOINT_ONLY] as boolean ?: false
		def q = params[SPARQL_QUERY_ENDPOINT_PROPERTY]
		def u = params[SPARQL_UPDATE_ENDPOINT_PROPERTY] ?: q
		def user = params[SPARQL_USERNAME_PROPERTY]
		def pw = params[SPARQL_PASSWORD_PROPERTY]
		
		return new RepositoryInit(remote, q, u, user, pw)
	}
	
	/**
	 * Builds an initialization object from environment variables.
	 * 
	 * @return a SPARQL endpoint initialization object
	 */
	private static RepositoryInit fromEnv() {
		def remote = System.getenv(REMOTE_SPARQL_ENDPOINT_ONLY) as boolean ?: false
		def q = System.getenv(SPARQL_QUERY_ENDPOINT_PROPERTY)
		def u = System.getenv(SPARQL_UPDATE_ENDPOINT_PROPERTY) ?: q
		def user = System.getenv(SPARQL_USERNAME_PROPERTY)
		def pw = System.getenv(SPARQL_PASSWORD_PROPERTY)
		
		return new RepositoryInit(remote, q, u, user, pw)
	}
	
}