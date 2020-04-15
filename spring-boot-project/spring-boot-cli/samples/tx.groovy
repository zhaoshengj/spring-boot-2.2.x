package org.test

@Grab("hsqldb")

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
class Example implements CommandLineRunner {

	@Autowired
	JdbcTemplate jdbcTemplate

	@Transactional
	void run(String... args) {
		println "Foo count=" + jdbcTemplate.queryForObject("SELECT COUNT(*) from FOO", Integer)
	}
}

