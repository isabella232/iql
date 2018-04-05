package com.indeed.imhotep.client;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.indeed.imhotep.AbstractImhotepMultiSession;
import com.indeed.imhotep.DatasetInfo;
import com.indeed.imhotep.GroupMultiRemapRule;
import com.indeed.imhotep.ImhotepRemoteSession;
import com.indeed.imhotep.Shard;
import com.indeed.imhotep.ShardInfo;
import com.indeed.imhotep.api.ImhotepOutOfMemoryException;
import com.indeed.imhotep.api.ImhotepSession;
import com.indeed.imhotep.local.ImhotepJavaLocalSession;
import com.indeed.imhotep.local.ImhotepLocalSession;
import com.indeed.imhotep.marshal.ImhotepDaemonMarshaller;
import com.indeed.imhotep.protobuf.GroupMultiRemapMessage;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Ignore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Ignore
public class TestImhotepClient extends ImhotepClient {
    private final List<com.indeed.squall.iql2.server.web.servlets.dataset.Shard> shards;

    public TestImhotepClient(List<com.indeed.squall.iql2.server.web.servlets.dataset.Shard> shards) {
        super(new DummyHostsReloader(Collections.<Host>emptyList()));
        this.shards = shards;

        final Map<String, DatasetInfo> datasetToDatasetInfo = new HashMap<>();
        for (final com.indeed.squall.iql2.server.web.servlets.dataset.Shard shard : shards) {
            final String dataset = shard.dataset;

            DatasetInfo datasetInfo = datasetToDatasetInfo.get(dataset);
            if(datasetInfo == null) {
                datasetInfo = new DatasetInfo(dataset, ((Collection<ShardInfo>)null),
                        Sets.newHashSet(), Sets.newHashSet(), 0L);
                datasetToDatasetInfo.put(dataset, datasetInfo);
            }
            datasetInfo.getIntFields().addAll(shard.flamdex.getIntFields());
            datasetInfo.getStringFields().addAll(shard.flamdex.getStringFields());
        }

        final Field datasetMetadataReloader;
        try {
            datasetMetadataReloader = ImhotepClient.class.getDeclaredField("datasetMetadataReloader");
            datasetMetadataReloader.setAccessible(true);

            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(datasetMetadataReloader, datasetMetadataReloader.getModifiers() & ~Modifier.FINAL);

            datasetMetadataReloader.set(this, new DumbImhotepClientMetadataReloader(datasetToDatasetInfo));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected List<Shard> getAllShardsForTimeRange(String dataset, DateTime start, DateTime end) {
        final List<Shard> result = new ArrayList<>();
        for(com.indeed.squall.iql2.server.web.servlets.dataset.Shard shard: shards) {
            if(!shard.dataset.equals(dataset)) {
                continue;
            }

            final Interval shardInterval = ShardTimeUtils.parseInterval(shard.shardId);
            if(shardInterval.overlaps(new Interval(start, end))) {
                // TODO: Not hardcode version to 2015-01-01 00:00:00?
                final Shard locatedShard = new Shard(shard.shardId, shard.flamdex.getNumDocs(), 20150101000000L);
                locatedShard.getServers().add(new Host("", 1));
                result.add(locatedShard);
            }
        }
        return result;
    }


        @Override
    public SessionBuilder sessionBuilder(final String dataset, DateTime start, DateTime end) {
        return new SessionBuilder(dataset, start, end) {
            private List<Shard> readShardsOverride() {
                try {
                    final Field shardsOverrideField = SessionBuilder.class.getDeclaredField("shardsOverride");
                    shardsOverrideField.setAccessible(true);
                    return (List<Shard>) shardsOverrideField.get(this);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw Throwables.propagate(e);
                }
            }

            @Override
            public ImhotepSession build() {
                final List<Shard> shardsOverride = readShardsOverride();
                final List<String> shardIds = Shard.keepShardIds(shardsOverride != null ? shardsOverride : this.getChosenShards());

                final List<ImhotepLocalSession> sessions = new ArrayList<>();

                for (final com.indeed.squall.iql2.server.web.servlets.dataset.Shard shard : TestImhotepClient.this.shards) {
                    if (shardIds.contains(shard.shardId) && shard.dataset.equals(dataset)) {
                        try {
                            sessions.add(new ImhotepJavaLocalSession(shard.flamdex));
                        } catch (ImhotepOutOfMemoryException e) {
                            throw Throwables.propagate(e);
                        }
                    }
                }

                return new AbstractImhotepMultiSession(sessions.toArray(new ImhotepSession[sessions.size()])) {
                    @Override
                    protected void postClose() {

                    }

                    @Override
                    public void writeFTGSIteratorSplit(String[] intFields, String[] stringFields, int splitIndex, int numSplits, long termLimit, Socket socket) throws ImhotepOutOfMemoryException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    protected ImhotepRemoteSession createImhotepRemoteSession(InetSocketAddress address, String sessionId, AtomicLong tempFileSizeBytesLeft) {
                        throw new UnsupportedOperationException();
                    }

                    //Workaround for regroupWithProtos to work in local (unit tests)
                    @Override
                    public int regroupWithProtos(GroupMultiRemapMessage[] rawRuleMessages, boolean errorOnCollisions) throws ImhotepOutOfMemoryException {
                        final GroupMultiRemapRule[] rules = ImhotepDaemonMarshaller.marshalGroupMultiRemapMessageList(Arrays.asList(rawRuleMessages));
                        return regroup(rules, errorOnCollisions);
                    }
                };
            }
        };
    }
}