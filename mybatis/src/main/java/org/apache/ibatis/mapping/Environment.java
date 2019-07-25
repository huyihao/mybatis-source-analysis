package org.apache.ibatis.mapping;

import javax.sql.DataSource;

import org.apache.ibatis.transaction.TransactionFactory;

public final class Environment {
	private final String id;
	private final TransactionFactory transactionFactory;
	private final DataSource dataSource;
	
	public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
		if (id == null) {
			throw new IllegalArgumentException("Parameter 'id' must not be null");
		}
		if (transactionFactory == null) {
			throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
		}
		this.id = id; 
		if (dataSource == null) {
			throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
		}
		this.transactionFactory = transactionFactory;
		this.dataSource = dataSource;
	}
	
	// 用来辅助构造Environment的类
	public static class Builder {
		private String id;
		private TransactionFactory transactionFactory;
		private DataSource dataSource;
		
		public Builder(String id) {
			this.id = id;
		}
		
		public Builder transactionFactory(TransactionFactory transactionFactory) {
			this.transactionFactory = transactionFactory;
			return this;
		}
		
		public Builder dataSource(DataSource dataSource) {
			this.dataSource = dataSource;
			return this;
		}
		
		public String id() {
			return this.id;
		}
		
		public Environment build() {
			return new Environment(this.id, this.transactionFactory, this.dataSource);
		}
	}
	
	public String id() {
		return this.id;
	}

	public TransactionFactory getTransactionFactory() {
		return transactionFactory;
	}

	public DataSource getDataSource() {
		return dataSource;
	}
}
