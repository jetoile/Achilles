package info.archinnov.achilles.columnFamily;

import static info.archinnov.achilles.dao.CounterDao.COUNTER_CF;
import info.archinnov.achilles.dao.GenericCompositeDao;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.ExternalWideMapProperties;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.exception.InvalidColumnFamilyException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ColumnFamilyHelper
 * 
 * @author DuyHai DOAN
 * 
 */
public class ColumnFamilyCreator {
    private static final Logger log = LoggerFactory.getLogger(ColumnFamilyCreator.class);
    private Cluster cluster;
    private Keyspace keyspace;
    private ColumnFamilyHelper columnFamilyHelper = new ColumnFamilyHelper();
    private List<ColumnFamilyDefinition> cfDefs;
    public static final Pattern CF_PATTERN = Pattern.compile("[a-zA-Z0-9_]{1,48}");
    private Set<String> cfs = new HashSet<String>();

    public ColumnFamilyCreator(Cluster cluster, Keyspace keyspace) {
        this.cluster = cluster;
        this.keyspace = keyspace;
        KeyspaceDefinition keyspaceDef = this.cluster.describeKeyspace(this.keyspace.getKeyspaceName());
        if (keyspaceDef != null && keyspaceDef.getCfDefs() != null) {
            cfDefs = keyspaceDef.getCfDefs();
        }
    }

    public ColumnFamilyDefinition discoverColumnFamily(String columnFamilyName) {
        log.debug("Start discovery of column family {}", columnFamilyName);
        for (ColumnFamilyDefinition cfDef : this.cfDefs) {
            if (StringUtils.equals(cfDef.getName(), columnFamilyName)) {
                log.debug("Existing column family {} found", columnFamilyName);
                return cfDef;
            }
        }
        return null;
    }

    public String addColumnFamily(ColumnFamilyDefinition cfDef) {
        this.cfs.add(cfDef.getName());
        return this.cluster.addColumnFamily(cfDef, true);
    }

    public void createColumnFamily(EntityMeta<?> entityMeta) {
        log.debug("Creating column family for entityMeta {}", entityMeta.getClassName());
        String columnFamilyName = entityMeta.getColumnFamilyName();
        if (!cfs.contains(columnFamilyName)) {
            ColumnFamilyDefinition cfDef;
            if (entityMeta.isColumnFamilyDirectMapping()) {

                PropertyMeta<?, ?> propertyMeta = entityMeta.getPropertyMetas().values().iterator().next();
                cfDef = this.columnFamilyHelper.buildCompositeCF(this.keyspace.getKeyspaceName(), propertyMeta,
                        entityMeta.getIdMeta().getValueClass(), columnFamilyName, entityMeta.getClassName());
            } else {
                cfDef = this.columnFamilyHelper.buildDynamicCompositeCF(entityMeta, this.keyspace.getKeyspaceName());

            }
            this.addColumnFamily(cfDef);
        }

    }

    public void validateOrCreateColumnFamilies(Map<Class<?>, EntityMeta<?>> entityMetaMap,
            boolean forceColumnFamilyCreation, boolean hasCounter) {
        for (Entry<Class<?>, EntityMeta<?>> entry : entityMetaMap.entrySet()) {

            EntityMeta<?> entityMeta = entry.getValue();
            for (Entry<String, PropertyMeta<?, ?>> entryMeta : entityMeta.getPropertyMetas().entrySet()) {
                PropertyMeta<?, ?> propertyMeta = entryMeta.getValue();

                ExternalWideMapProperties<?> externalWideMapProperties = propertyMeta.getExternalWideMapProperties();
                if (externalWideMapProperties != null) {
                    GenericCompositeDao<?, ?> externalWideMapDao = externalWideMapProperties.getExternalWideMapDao();
                    this.validateOrCreateCFForExternalWideMap(propertyMeta, entityMeta.getIdMeta().getValueClass(),
                            forceColumnFamilyCreation, externalWideMapDao.getColumnFamily(),
                            entityMeta.getClassName());
                }
            }

            this.validateOrCreateCFForEntity(entityMeta, forceColumnFamilyCreation);
        }

        if (hasCounter) {
            this.validateOrCreateCFForCounter(forceColumnFamilyCreation);
        }
    }

    private <ID> void validateOrCreateCFForExternalWideMap(PropertyMeta<?, ?> propertyMeta, Class<ID> keyClass,
            boolean forceColumnFamilyCreation, String externalColumnFamilyName, String entityName) {

        ColumnFamilyDefinition cfDef = this.discoverColumnFamily(externalColumnFamilyName);
        if (cfDef == null) {
            if (forceColumnFamilyCreation) {
                log.debug("Force creation of column family for propertyMeta {}", propertyMeta.getPropertyName());

                cfDef = this.columnFamilyHelper.buildCompositeCF(this.keyspace.getKeyspaceName(), propertyMeta,
                        keyClass, externalColumnFamilyName, entityName);
                this.addColumnFamily(cfDef);
            } else {
                throw new InvalidColumnFamilyException("The required column family '" + externalColumnFamilyName
                        + "' does not exist for field '" + propertyMeta.getPropertyName() + "'");
            }
        } else {
            this.columnFamilyHelper.validateCFWithPropertyMeta(cfDef, propertyMeta, externalColumnFamilyName);
        }
    }

    public void validateOrCreateCFForEntity(EntityMeta<?> entityMeta, boolean forceColumnFamilyCreation) {
        ColumnFamilyDefinition cfDef = this.discoverColumnFamily(entityMeta.getColumnFamilyName());
        if (cfDef == null) {
            if (forceColumnFamilyCreation) {
                log.debug("Force creation of column family for entityMeta {}", entityMeta.getClassName());

                this.createColumnFamily(entityMeta);
            } else {
                throw new InvalidColumnFamilyException("The required column family '"
                        + entityMeta.getColumnFamilyName() + "' does not exist for entity '"
                        + entityMeta.getClassName() + "'");
            }
        } else {
            this.columnFamilyHelper.validateCFWithEntityMeta(cfDef, entityMeta);
        }
    }

    private void validateOrCreateCFForCounter(boolean forceColumnFamilyCreation) {
        ColumnFamilyDefinition cfDef = this.discoverColumnFamily(COUNTER_CF);
        if (cfDef == null) {
            if (forceColumnFamilyCreation) {
                log.debug("Force creation of column family for counters");

                this.createCounterColumnFamily();
            } else {
                throw new InvalidColumnFamilyException("The required column family '" + COUNTER_CF
                        + "' does not exist");
            }
        } else {
            this.columnFamilyHelper.validateCounterCF(cfDef);
        }

    }

    private void createCounterColumnFamily() {
        log.debug("Creating counter column family");
        if (!cfs.contains(COUNTER_CF)) {
            ColumnFamilyDefinition cfDef = columnFamilyHelper.buildCounterCF(this.keyspace.getKeyspaceName());
            this.addColumnFamily(cfDef);
        }
    }

}
