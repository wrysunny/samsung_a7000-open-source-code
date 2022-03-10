/*
 * Copyright (c) 2012 Global Radio UK Limited
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
 */

package org.radiodns;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Represents a Radio Service from which RadioDNS Applications can be resolved
 * 
 * @author Byrion Smith <byrion.smith@thisisglobal.com>
 * @version 1.0.2
 */
public abstract class Service {

    String mDNSHostname = null;

    /**
     * Get RadioDNS FQDN
     * 
     * @return RadioDNS FQDN
     */
    public abstract String getRadioDNSFqdn();

    int cname_result = Lookup.SUCCESSFUL;
    int srvRecord_result = Lookup.SUCCESSFUL;

    long cname_ttl = -1;
    long srvRecord_ttl = -1;

    /**
     * Get Authoritative FQDN
     * 
     * @return Authoritative FQDN
     * @throws LookupException
     */
    public String getAuthoritativeFqdn() throws LookupException {
        return resolveAuthoritativeFQDN();
    }

    /**
     * Get given RadioDNS Application
     * 
     * @param applicationId
     *            RadioDNS Application Identifier
     * @return RadioDNS Application
     * @throws LookupException
     */
    public Application getApplication(String applicationId) throws LookupException {
        return resolveApplication(applicationId);
    }

    public Application getApplication(String cname, String applicationId) throws LookupException {
        return resolveApplication(cname, applicationId, "TCP");
    }

    /**
     * Get all known RadioDNS Applications
     * 
     * @return Map of RadioDNS Applications
     * @throws LookupException
     */
    public Map<String, Application> getApplications() throws LookupException {
        Map<String, Application> applications = new HashMap<String, Application>();
        for (String applicationId : RadioDNS.KNOWN_APPLICATIONS) {
            applications.put(applicationId, resolveApplication(applicationId));
        }
        return applications;
    }

    /**
     * Supply a DNS server hostname to query when performing lookups
     * 
     * @param hostname
     *            DNS Server to query
     */
    public void setDNSHostname(String hostname) {
        mDNSHostname = hostname;
    }

    /**
     * Get RadioVIS Application for the given Application ID
     * 
     * @param applicationId
     *            RadioDNS Application Identifier
     * @return
     * @throws LookupException
     */
    Application resolveApplication(String applicationId) throws LookupException {
        return resolveApplication(applicationId, null);
    }

    /**
     * Get RadioVIS Application for the given Application ID and Transport
     * Protocol
     * 
     * @param applicationId
     *            RadioDNS Application Identifier
     * @param transportProtocol
     *            Transport Protocol
     * @return
     * @throws LookupException
     */
    Application resolveApplication(String applicationId, String transportProtocol)
            throws LookupException {
        String authoritativeFqdn = getAuthoritativeFqdn();
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID is null");
        }
        if (transportProtocol == null) {
            transportProtocol = "TCP";
        }

        if (authoritativeFqdn == null) {
            System.out.println("authoritativeFqdn == null");
            return null;
        }

        return resolveApplication(authoritativeFqdn, applicationId, transportProtocol);
    }

    Application resolveApplication(String authoritativeFqdn, String applicationId,
            String transportProtocol) throws LookupException {
        String applicationFqdn = String.format("_%s._%s.%s", applicationId.toLowerCase(),
                transportProtocol.toLowerCase(), authoritativeFqdn);

        SimpleResolver resolver;
        try {
            if (mDNSHostname == null) {
                resolver = new SimpleResolver();
            } else {
                resolver = new SimpleResolver(mDNSHostname);
            }
        } catch (UnknownHostException e) {
            throw new LookupException("Error creating DNS Resolver", e);
        }

        Lookup lookup;
        try {
            lookup = new Lookup(applicationFqdn, Type.SRV);
        } catch (TextParseException e) {
            throw new LookupException("Error parsing DNS response", e);
        }
        lookup.setResolver(resolver);
        org.xbill.DNS.Record[] records = lookup.run();

        srvRecord_result = lookup.getResult();

        if (records != null) {
            List<Record> servers = new ArrayList<Record>();
            for (org.xbill.DNS.Record r : records) {
                if (r.getType() == Type.SRV) {
                    srvRecord_ttl = r.getTTL();
                    servers.add(new Record((SRVRecord) r));
                }
            }

            return new Application(applicationId.toLowerCase(), servers);
        }

        return null;
    }

    public int getSrvRecord_result() {
        return srvRecord_result;
    }

    public int getCname_result() {
        return cname_result;
    }

    public long getCname_ttl() {
        return cname_ttl;
    }

    public long getSrvRecord_ttl() {
        return srvRecord_ttl;
    }

    /**
     * Resolve Authoritative FQDN for the service
     * 
     * @return Authoritative FQDN String
     * @throws LookupException
     */
    String resolveAuthoritativeFQDN() throws LookupException {
        try {
            SimpleResolver resolver;
            if (mDNSHostname == null) {
                resolver = new SimpleResolver();
            } else {
                resolver = new SimpleResolver(mDNSHostname);
            }
            Lookup lookup = new Lookup(getRadioDNSFqdn(), Type.CNAME);
            lookup.setResolver(resolver);
            org.xbill.DNS.Record[] records = lookup.run();

            cname_result = lookup.getResult();

            if (records != null) {
                for (org.xbill.DNS.Record record : records) {
                    if (record.getType() == Type.CNAME) {
                        cname_ttl = record.getTTL();
                        return ((CNAMERecord) record).getTarget().toString();
                    }
                }
            } else {
                System.out.println("resolveAuthoritativeFQDN() records are Null");
            }
        } catch (UnknownHostException e) {
            throw new LookupException("Error creating DNS Resolver", e);
        } catch (TextParseException e) {
            throw new LookupException("Error parsing DNS response", e);
        }

        return null;
    }
}