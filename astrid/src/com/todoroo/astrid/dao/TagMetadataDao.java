/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagMetadata;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagMetadataDao extends DatabaseDao<TagMetadata> {

    @Autowired
    private Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public TagMetadataDao() {
        super(TagMetadata.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    public void createMemberLink(String tagUuid, String memberId) {
        // TODO: Implement
    }

    public void removeMemberLinks(String tagUuid, String[] memberIds) {
        // TODO: Implement
    }
}

