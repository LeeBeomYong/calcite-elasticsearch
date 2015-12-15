package org.apache.calcite.adapter.elasticsearch;


import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.calcite.adapter.java.AbstractQueryableTable;
import org.apache.calcite.linq4j.*;
import org.apache.calcite.linq4j.function.Function1;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.*;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.impl.AbstractTableQueryable;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.metamodel.DataContext;
import org.apache.metamodel.UpdateableDataContext;
import org.apache.metamodel.schema.*;


public class ElasticsearchTable extends AbstractQueryableTable
        implements TranslatableTable {
    protected final String tableName;
    protected final Table table;
    private final RelProtoDataType protoRowType;
    private final DataContext esdc;
    int[] fields;

    /**
     * Creates a ElasticsearchTable.
     */
    ElasticsearchTable(Table table, DataContext esdc) {
        super(Object[].class);
        this.table = table;
        this.tableName = table.getName();
        this.esdc = esdc;

        RelDataTypeFactory typeFactory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
        RelDataTypeFactory.FieldInfoBuilder fieldInfo = typeFactory.builder();

        for (Column column : table.getColumns()) {
            String columnName = column.getName();
            //ColumnType columnType = column.getType();
            ColumnType columnType = ColumnType.VARCHAR;
            try {
               int jdbcType = columnType.getJdbcType();
                SqlTypeName typeName = SqlTypeName.getNameForJdbcType(jdbcType);
                RelDataType type = typeFactory.createSqlType(typeName);
                fieldInfo.add(columnName, type);
            } catch (Exception e) {
            }
        }
        protoRowType = RelDataTypeImpl.proto(fieldInfo.build());
    }

    public String toString() {
        return "ElasticsearchTable {" + tableName + "}";
    }

    public <T> Queryable<T> asQueryable(QueryProvider queryProvider,
                                        SchemaPlus schema, String tableName) {
        // TODO Auto-generated method stub
        return new ElasticsearchQueryable<T>(queryProvider, schema, this, tableName);
    }

    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        // TODO Auto-generated method stub
        return protoRowType.apply(typeFactory);
    }

    /**
     * Returns an enumerable over a given projection of the fields.
     * <p/>
     * <p>Called from generated code.
     */
    public Enumerable<Object> project(int[] fields) {
        fields = this.fields;
        final int[] finalFields = fields;
        return new AbstractEnumerable<Object>() {
            public Enumerator<Object> enumerator() {
                return new ElastaicsearchEnumerator(esdc, table, finalFields);
            }
        };
    }

    public RelNode toRel(
            RelOptTable.ToRelContext context,
            RelOptTable relOptTable) {
        // Request all fields.
        final int fieldCount = relOptTable.getRowType().getFieldCount();
        fields = ElastaicsearchEnumerator.identityList(fieldCount);
        return new ElasticsearchTableScan(context.getCluster(), relOptTable, this, fields);
    }

    public Expression getExpression(SchemaPlus schema, String tableName,
                                    Class clazz) {
        return Schemas.tableExpression(schema, getElementType(), tableName, clazz);
    }

    public Enumerable<Object> find(UpdateableDataContext esdc, String filterJson,
                                   String projectJson, List<Map.Entry<String, Class>> fields) {
        filterJson = null;
        projectJson = null;
        final Schema schema = esdc.getDefaultSchema();
        final Table elaTable = schema.getTableByName("testcreatetable");
        final Function1<Table, Object> getter = ElasticsearchEnumerator1.getter(fields);
        return new AbstractEnumerable<Object>() {
            public Enumerator<Object> enumerator() {
                return new ElasticsearchEnumerator1((Iterator<Table>) elaTable, getter);
            }
        };
    }

    public static class ElasticsearchQueryable<T> extends AbstractTableQueryable<T> {
        public ElasticsearchQueryable(QueryProvider queryProvider, SchemaPlus schema,
                                      ElasticsearchTable table, String tableName) {
            super(queryProvider, schema, table, tableName);
        }

        public Enumerator<T> enumerator() {
            //noinspection unchecked
            final Enumerable<T> enumerable =
                    (Enumerable<T>) getTable().project(null);
            return enumerable.enumerator();
        }

        private UpdateableDataContext getElasticsearch() {
            return schema.unwrap(ElasticsearchSchema.class).esdc;
        }

        private ElasticsearchTable getTable() {
            return (ElasticsearchTable) table;
        }

    }
}
