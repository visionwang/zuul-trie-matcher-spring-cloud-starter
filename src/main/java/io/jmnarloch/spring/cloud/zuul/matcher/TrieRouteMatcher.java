/**
 * Copyright (c) 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jmnarloch.spring.cloud.zuul.matcher;

import io.jmnarloch.spring.cloud.zuul.trie.Trie;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Trie based {@link RouteMatcher}.
 *
 * @author Jakub Narloch
 */
public class TrieRouteMatcher implements RouteMatcher {

    /**
     * The suffix used for wildcard route matching.
     */
    private static final String WILDCARD = "**";

    /**
     * The instance of {@link TrieSupplier} used for instantiating new Tries.
     */
    private final TrieSupplier trieSupplier;

    /**
     * Holds the reference to the Trie instance.
     */
    private final AtomicReference<Trie<ZuulRouteEntry>> trie =
            new AtomicReference<Trie<ZuulRouteEntry>>();

    /**
     * Creates new instance of {@link TrieRouteMatcher} with specific supplier.
     *
     * @param trieSupplier the Trie instance supplier
     */
    public TrieRouteMatcher(TrieSupplier trieSupplier) {
        Assert.notNull(trieSupplier, "Parameter 'trieSupplier' can not be null");
        this.trieSupplier = trieSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRoutes(Map<String, ZuulProperties.ZuulRoute> routes) {

        final Trie<ZuulRouteEntry> trie = createTrie();
        for (Map.Entry<String, ZuulProperties.ZuulRoute> route : routes.entrySet()) {
            trie.put(
                    path(route.getKey()),
                    new ZuulRouteEntry(route.getKey(), route.getValue(), isWildcard(route.getKey()))
            );
        }
        this.trie.lazySet(trie);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ZuulProperties.ZuulRoute getMatchingRoute(String path) {
        final ZuulRouteEntry matching = trie.get().prefix(path);
        if (matching == null
                || !matching.isWildcard() && !matchesExact(path, matching.getPath())) {
            return null;
        } else {
            return matching.getRoute();
        }
    }

    /**
     * Normalizes the path by removing any wildcard symbol front the end.
     *
     * @param path the path
     * @return the normalized path
     */
    private String path(String path) {
        if (isWildcard(path)) {
            path = path.substring(0, path.length() - WILDCARD.length());
        }
        return path;
    }

    /**
     * Returns whether the actual request path matches the configured route.
     *
     * @param expected the configured path
     * @param actual   the request path
     * @return true if actual path matches the expectation
     */
    private boolean matchesExact(String expected, String actual) {
        return expected.length() == actual.length();
    }

    /**
     * Returns whether the specific path contains a wildcard.
     *
     * @param path the path
     * @return whether path contains wildcard
     */
    private boolean isWildcard(String path) {
        return path.endsWith(WILDCARD);
    }

    /**
     * Creates new instance of {@link Trie} by delegating to the provided {@link TrieSupplier} instance.
     *
     * @return the trie instance
     */
    private Trie<ZuulRouteEntry> createTrie() {
        return trieSupplier.createTrie();
    }

    /**
     * A simple wrapper on the Trie value entry allowing to associate additional information with the route specs.
     *
     * @author Jakub Narloch
     */
    private static class ZuulRouteEntry {

        /**
         * The route path.
         */
        private final String path;

        /**
         * The route spec.
         */
        private final ZuulProperties.ZuulRoute route;

        /**
         * Whether the route is a wildcard.
         */
        private final boolean wildcard;

        /**
         * Creates new instance of {@link ZuulRouteEntry}
         *
         * @param path           the route path
         * @param route the zuul route
         * @param wildcard       whether the route is wildcard
         */
        public ZuulRouteEntry(String path, ZuulProperties.ZuulRoute route, boolean wildcard) {
            this.path = path;
            this.route = route;
            this.wildcard = wildcard;
        }

        /**
         * Returns the route path.
         *
         * @return the route path
         */
        public String getPath() {
            return path;
        }

        /**
         * Retrieves the route spec
         *
         * @return the route spec
         */
        public ZuulProperties.ZuulRoute getRoute() {
            return route;
        }

        /**
         * Returns whether the path is a wildcard.
         *
         * @return the path wildcard
         */
        public boolean isWildcard() {
            return wildcard;
        }
    }

    /**
     * The Trie instance supplier, used whenever to instantiate and populate a Trie whenever a new list of routes is
     * being provided.
     *
     * @author Jakub Narloch
     */
    public interface TrieSupplier {

        <T> Trie<T> createTrie();
    }
}
