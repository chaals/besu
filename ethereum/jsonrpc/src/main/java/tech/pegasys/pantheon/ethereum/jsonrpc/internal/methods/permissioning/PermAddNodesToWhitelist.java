/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.permissioning;

import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.StringListParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.p2p.P2pDisabledException;
import tech.pegasys.pantheon.ethereum.permissioning.NodeLocalConfigPermissioningController;

import java.util.List;
import java.util.Optional;

public class PermAddNodesToWhitelist implements JsonRpcMethod {

  private final Optional<NodeLocalConfigPermissioningController>
      nodeWhitelistPermissioningController;
  private final JsonRpcParameter parameters;

  public PermAddNodesToWhitelist(
      final Optional<NodeLocalConfigPermissioningController> nodeWhitelistPermissioningController,
      final JsonRpcParameter parameters) {
    this.nodeWhitelistPermissioningController = nodeWhitelistPermissioningController;
    this.parameters = parameters;
  }

  @Override
  public String getName() {
    return RpcMethod.PERM_ADD_NODES_TO_WHITELIST.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest req) {
    final StringListParameter enodeListParam =
        parameters.required(req.getParams(), 0, StringListParameter.class);

    try {
      if (nodeWhitelistPermissioningController.isPresent()) {
        try {
          final List<String> enodeURLs = enodeListParam.getStringList();
          final NodeLocalConfigPermissioningController.NodesWhitelistResult nodesWhitelistResult =
              nodeWhitelistPermissioningController.get().addNodes(enodeURLs);

          switch (nodesWhitelistResult.result()) {
            case SUCCESS:
              return new JsonRpcSuccessResponse(req.getId());
            case ERROR_EMPTY_ENTRY:
              return new JsonRpcErrorResponse(req.getId(), JsonRpcError.NODE_WHITELIST_EMPTY_ENTRY);
            case ERROR_EXISTING_ENTRY:
              return new JsonRpcErrorResponse(
                  req.getId(), JsonRpcError.NODE_WHITELIST_EXISTING_ENTRY);
            case ERROR_DUPLICATED_ENTRY:
              return new JsonRpcErrorResponse(
                  req.getId(), JsonRpcError.NODE_WHITELIST_DUPLICATED_ENTRY);
            case ERROR_WHITELIST_PERSIST_FAIL:
              return new JsonRpcErrorResponse(req.getId(), JsonRpcError.WHITELIST_PERSIST_FAILURE);
            case ERROR_WHITELIST_FILE_SYNC:
              return new JsonRpcErrorResponse(req.getId(), JsonRpcError.WHITELIST_FILE_SYNC);
            default:
              throw new Exception();
          }
        } catch (IllegalArgumentException e) {
          return new JsonRpcErrorResponse(req.getId(), JsonRpcError.NODE_WHITELIST_INVALID_ENTRY);
        } catch (Exception e) {
          return new JsonRpcErrorResponse(req.getId(), JsonRpcError.INTERNAL_ERROR);
        }
      } else {
        return new JsonRpcErrorResponse(req.getId(), JsonRpcError.NODE_WHITELIST_NOT_ENABLED);
      }
    } catch (P2pDisabledException e) {
      return new JsonRpcErrorResponse(req.getId(), JsonRpcError.P2P_DISABLED);
    }
  }
}
