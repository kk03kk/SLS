# 测试逐文件索引

从当前测试源码提取；函数数量不是参数化展开后的用例数量。所有下列源码和小型 fixture 均作为正式项目保留并上传。完整用途与运行边界见 [README](README.md)。

## (root)

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [conftest.py](conftest.py) | 0 | 数据/辅助入口；见目录说明 |
| [test_a20_act1.py](test_a20_act1.py) | 14 | stock_start_thresholds_and_v5_observation<br>a20_three_act1_bosses_stop_before_reward_decisions |
| [test_a20_curriculum.py](test_a20_curriculum.py) | 3 | a20_curriculum_keeps_keys_and_stops_at_its_actual_horizon<br>a20_heart_without_keys_cannot_count_act3_as_success |
| [test_act12_analysis.py](test_act12_analysis.py) | 9 | compare_all_normal_starts_even_when_reach_rates_differ<br>act1_clear_is_not_an_act12_success |
| [test_act12_pilot.py](test_act12_pilot.py) | 15 | build_pilot_has_new_goal_fresh_transfer_and_unchanged_ppo<br>bad_run_names_rejected |
| [test_act1_training.py](test_act1_training.py) | 15 | note_auto_leave_matches_manual_leave_and_restores_without_policy_step<br>act1_keys_have_real_opportunity_cost_and_no_reward_bonus |
| [test_benchmark_workers.py](test_benchmark_workers.py) | 4 | benchmark_defaults_are_training_only<br>production_layouts_exercise_all_allocated_cpu_shards |
| [test_build_native.py](test_build_native.py) | 5 | cmake_uses_packaged_binary_on_every_platform<br>linux_configure_uses_same_required_cmake_binary |
| [test_build_oracle.py](test_build_oracle.py) | 7 | committed_oracle_resources_are_complete_and_unique<br>jar_is_deterministic_across_member_insertion_orders |
| [test_compare_run_arms.py](test_compare_run_arms.py) | 7 | mcnemar_and_wilson_match_closed_forms<br>identical_arms_report_no_difference |
| [test_configure_live_inspector.py](test_configure_live_inspector.py) | 4 | configure_live_inspector_backs_up_preserves_and_restores<br>configure_refuses_unrecognized_config_without_creating_backup |
| [test_curriculum.py](test_curriculum.py) | 9 | act_one_requires_explicit_boss_completion_before_act_change<br>act_one_completes_on_entry_to_act_two |
| [test_event_observation.py](test_event_observation.py) | 11 | event_details_are_encodable_and_only_displayed_fields<br>falling_action_identifies_exact_removed_card |
| [test_intervene_policy_decision.py](test_intervene_policy_decision.py) | 7 | selector_parsing_rejects_malformed_specs<br>option_selector_matches_only_the_named_offer |
| [test_neow_observation.py](test_neow_observation.py) | 5 | native_enum_mapping_matches_source<br>offer_features_are_semantic_and_do_not_contain_outcomes |
| [test_oracle_build_tools.py](test_oracle_build_tools.py) | 3 | oracle_expansion_preserves_source_and_unrelated_members<br>oracle_rejects_same_resolved_output_without_touching_source |
| [test_structure.py](test_structure.py) | 2 | legacy_architectures_are_absent<br>canonical_assets_are_present |
| [test_submit_slurm.py](test_submit_slurm.py) | 14 | all_tasks_preserve_virtualenv_python_path<br>nus_command_matrix |
| [test_tool_checks.py](test_tool_checks.py) | 5 | config_check_supports_external_root<br>registry_check_is_read_only_even_when_stale |
| [test_training_entrypoint_audit.py](test_training_entrypoint_audit.py) | 9 | warm_start_retention_gate_rejects_weak_or_invalid_baseline<br>local_migration_report_cannot_authorize_server_training |
| [test_training_runtime.py](test_training_runtime.py) | 20 | train_can_resume_finalization_after_target<br>completed_or_nontrain_stage_cannot_resume_finalization |
| [test_training_source_reuse.py](test_training_source_reuse.py) | 5 | real_nus_validation_report_was_reusable_for_reviewed_code<br>old_validation_cannot_authorize_new_neow_encoding |
| [test_win_continuation_analysis.py](test_win_continuation_analysis.py) | 6 | paired_continuation_recomputes_raw_changes<br>paired_continuation_refuses_contract_mismatch |

## audit

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [audit/test_act1_map.py](audit/test_act1_map.py) | 2 | act1_map_chances_match_stock_projection<br>act1_maps_respect_stock_room_boundaries |
| [audit/test_act1_regression_corpus.py](audit/test_act1_regression_corpus.py) | 3 | corpus_rejects_changed_stock_evidence<br>corpus_cannot_count_one_route_twice |
| [audit/test_act1_targets.py](audit/test_act1_targets.py) | 6 | a20_act1_targets_exclude_ineligible_events_and_later_acts<br>coverage_builder_uses_target_inventory_without_claiming_parity |
| [audit/test_bytecode_inventory.py](audit/test_bytecode_inventory.py) | 3 | class_names_only_returns_top_level_package_classes<br>source_reference_index_scans_each_enum_reference |
| [audit/test_card_parity.py](audit/test_card_parity.py) | 1 | structured_differences_retains_exact_paths_and_values |
| [audit/test_encounter_parity.py](audit/test_encounter_parity.py) | 1 | encounter_parser_retains_pre_constructor_rng_boundary |
| [audit/test_event_parity.py](audit/test_event_parity.py) | 1 | event_parser_retains_constructor_rng_boundary |
| [audit/test_mechanics_normalization.py](audit/test_mechanics_normalization.py) | 4 | java_unsigned_rng_state_is_preserved_without_float_conversion<br>json_float32_roundtrip_is_compared_by_bits |
| [audit/test_mechanism_parity.py](audit/test_mechanism_parity.py) | 1 | mechanism_payloads_only_collects_supported_scenarios |
| [audit/test_relic_parity.py](audit/test_relic_parity.py) | 2 | trigger_projection_excludes_oracle_batch_state_leakage<br>only_documented_equip_timing_fields_are_boundaries |
| [audit/test_semantic_coverage.py](audit/test_semantic_coverage.py) | 9 | complete_independent_obligation_passes_gate<br>partial_branch_blocks_training |
| [audit/test_stock_parity.py](audit/test_stock_parity.py) | 2 | original_capture_parser_does_not_confuse_capture_with_parity<br>manifest_inventory_and_evidence_statuses |

## content

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [content/test_card_features.py](content/test_card_features.py) | 7 | dynamic_damage_and_flags_agree_between_adapters<br>dynamic_damage_changes_model_input |
| [content/test_energy.py](content/test_energy.py) | 3 | permanent_energy_relic_is_visible_at_every_boundary<br>explicit_combat_energy_is_authoritative |
| [content/test_registry.py](content/test_registry.py) | 8 | committed_registry_is_valid<br>original_internal_ids_normalize_through_the_registry |
| [content/test_scope.py](content/test_scope.py) | 6 | committed_ironclad_scope_is_current_and_exactly_scoped<br>scope_file_digest_is_deterministic |
| [content/test_seed.py](content/test_seed.py) | 2 | seed_conversion_round_trips_unsigned_long_bits<br>seed_input_matches_original_ui_normalization |

## contracts

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [contracts/test_action.py](contracts/test_action.py) | 3 | candidate_identity_is_semantic_and_stable<br>backend_metadata_cannot_enter_an_action |
| [contracts/test_decision.py](contracts/test_decision.py) | 3 | duplicate_candidates_are_rejected<br>transition_and_decision_termination_agree |
| [contracts/test_observation.py](contracts/test_observation.py) | 2 | observation_contains_no_action_set<br>hidden_draw_order_is_rejected |

## diagnostics

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [diagnostics/test_a20_macro_diagnostic.py](diagnostics/test_a20_macro_diagnostic.py) | 2 | map_action_resolves_visible_room_type<br>summary_deduplicates_card_offers_and_classifies_outcomes |
| [diagnostics/test_act1_corpus_selection.py](diagnostics/test_act1_corpus_selection.py) | 2 | corpus_selection_is_order_independent_and_has_declared_strata<br>sparse_current_cohort_keeps_available_strata_without_duplicate_padding |
| [diagnostics/test_act1_failure_analysis.py](diagnostics/test_act1_failure_analysis.py) | 2 | failure_analysis_separates_boss_early_and_elite_deaths<br>failure_analysis_does_not_mislabel_runtime_failure_as_death |
| [diagnostics/test_act1_plateau.py](diagnostics/test_act1_plateau.py) | 5 | current_evaluation_rejects_wrong_environment_partial_and_unsafe_results<br>plateau_config_excludes_development_seeds_from_training_and_protects_source |
| [diagnostics/test_canary.py](diagnostics/test_canary.py) | 12 | tensor_hash_is_bit_stable<br>capture_uses_same_recurrent_context_as_live_runtime |
| [diagnostics/test_fullrun_evidence.py](diagnostics/test_fullrun_evidence.py) | 6 | complete_distinct_evidence_passes<br>repeated_report_cannot_satisfy_independent_trajectory_count |
| [diagnostics/test_original_canary_launcher.py](diagnostics/test_original_canary_launcher.py) | 9 | original_runtime_uses_explicit_game_path<br>original_runtime_discovers_steam_instead_of_fixed_drive |
| [diagnostics/test_paired_checkpoint_evaluation.py](diagnostics/test_paired_checkpoint_evaluation.py) | 5 | paired_input_rejects_empty_or_non_boolean_outcomes<br>paired_comparison_counts_transitions_and_boss_groups |
| [diagnostics/test_seed_audit.py](diagnostics/test_seed_audit.py) | 2 | seed_audit_reproduces_signed_run_and_records_counterfactual<br>current_seed_audit_uses_artifact_environment_and_signed_seed |

## fixtures

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [fixtures/act1-transform-selection-stock.json](fixtures/act1-transform-selection-stock.json) | — | 数据/辅助入口；见目录说明 |
| [fixtures/regressions/act1-note-seed-3000000000025.json](fixtures/regressions/act1-note-seed-3000000000025.json) | — | 数据/辅助入口；见目录说明 |
| [fixtures/regressions/act1-note-seed-3000000000047.json](fixtures/regressions/act1-note-seed-3000000000047.json) | — | 数据/辅助入口；见目录说明 |
| [fixtures/regressions/nus-worker-23-seed-8335-invalid-decision.json](fixtures/regressions/nus-worker-23-seed-8335-invalid-decision.json) | — | 数据/辅助入口；见目录说明 |
| [fixtures/regressions/original-purity-multi-select.json.gz](fixtures/regressions/original-purity-multi-select.json.gz) | — | 数据/辅助入口；见目录说明 |
| [fixtures/regressions/potion-during-card-choice.json.gz](fixtures/regressions/potion-during-card-choice.json.gz) | — | 数据/辅助入口；见目录说明 |

## model

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [model/test_match_pair_references.py](model/test_match_pair_references.py) | 3 | match_pair_encoding_contains_both_slots_and_their_known_cards<br>hidden_match_cards_are_not_used_as_pair_features |
| [model/test_policy.py](model/test_policy.py) | 19 | policy_scores_the_current_candidate_set<br>committed_policy_vocabulary_matches_native_sources |
| [model/test_power_ownership.py](model/test_power_ownership.py) | 4 | missing_or_dangling_power_owner_is_rejected<br>both_adapters_supply_same_owner |
| [model/test_selected_cards.py](model/test_selected_cards.py) | 2 | selected_cards_keep_order_and_public_properties_without_remaining_choices<br>selected_card_identity_and_order_change_model_input |
| [model/test_selection_action_references.py](model/test_selection_action_references.py) | 4 | standalone_bowl_index_five_is_not_a_select_card_reference<br>grid_selection_index_five_is_a_real_card_across_selection_types |

## oracle

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [oracle/OracleModeProbe.java](oracle/OracleModeProbe.java) | — | 数据/辅助入口；见目录说明 |

## original

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [original/test_adapter.py](original/test_adapter.py) | 15 | map_choices_become_semantic_actions<br>prismatic_shard_is_policy_hidden_without_shifting_shop_commands |
| [original/test_live_backend.py](original/test_live_backend.py) | 8 | live_backend_rejects_unsafe_or_unknown_oracle_before_actions<br>live_backend_attaches_without_resetting_or_starting |
| [original/test_original_backend.py](original/test_original_backend.py) | 39 | reset_folds_the_original_only_neow_dialog<br>reset_waits_for_asynchronous_run_start |
| [original/test_transport.py](original/test_transport.py) | 2 | stdio_transport_times_out_when_original_stops_replying<br>stdio_transport_decodes_localized_game_json_as_utf8 |

## rl

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [rl/test_act1_continuation.py](rl/test_act1_continuation.py) | 8 | interrupted_parent_requires_explicit_complete_periodic_evidence<br>continuation_preserves_learning_and_refuses_unreviewed_changes |
| [rl/test_advantage_domain_diagnostics.py](rl/test_advantage_domain_diagnostics.py) | 3 | decision_domains_matches_rollout_shape<br>domain_normalization_gives_each_domain_unit_standard_deviation |
| [rl/test_best_checkpoint.py](rl/test_best_checkpoint.py) | 5 | rank_prefers_act_progress_then_failure_floor_and_stability<br>one_extra_win_does_not_hide_observed_7m_to_10m_act_regression |
| [rl/test_checkpoint_policy.py](rl/test_checkpoint_policy.py) | 5 | 6b2fe23_style_checkpoint_model_contract_loads_safely<br>constructor_only_model_contract_also_loads_safely |
| [rl/test_curriculum_simulator_transition.py](rl/test_curriculum_simulator_transition.py) | 5 | unchanged_simulator_needs_no_migration<br>changed_rules_require_explicit_review |
| [rl/test_endpoint_continuation.py](rl/test_endpoint_continuation.py) | 1 | complete_endpoint_retains_all_learning_state_and_resets_selection |
| [rl/test_episode_limit.py](rl/test_episode_limit.py) | 4 | policy_boundary_fingerprint_ignores_candidate_and_entity_order<br>episode_limit_allows_four_visits_and_fails_on_fourth_repeat |
| [rl/test_evaluation_identity.py](rl/test_evaluation_identity.py) | 3 | config_digest_ignores_line_ending_convention<br>config_digest_is_byte_identical_to_raw_hash_on_lf_checkout |
| [rl/test_evaluation_semantics.py](rl/test_evaluation_semantics.py) | 7 | evaluation_success_is_a_real_fullrun_victory<br>evaluation_failure_reward_includes_terminal_floor |
| [rl/test_model_migration.py](rl/test_model_migration.py) | 3 | transfer_preserves_old_projection_including_presence_mask_offset<br>full_actor_critic_and_recurrent_output_preserved_on_old_input_subspace |
| [rl/test_ppo_math.py](rl/test_ppo_math.py) | 7 | coalesced_cpu_preserves_tensor_order_shape_dtype_and_values<br>advantages_are_normalized_once_over_the_complete_rollout |
| [rl/test_reward.py](rl/test_reward.py) | 8 | act_one_potential_matches_the_committed_formula<br>potential_shaping_preserves_terminal_reward_component |
| [rl/test_rollout.py](rl/test_rollout.py) | 2 | gae_does_not_cross_terminal_boundaries<br>gae_bootstraps_only_the_open_rollout_boundary |
| [rl/test_runtime_rebind_policy.py](rl/test_runtime_rebind_policy.py) | 8 | job_821775_marketing_name_is_recorded_rebind<br>semantic_or_unvalidated_numerical_changes_remain_strict |
| [rl/test_single_act_selection.py](rl/test_single_act_selection.py) | 9 | clear_count_guard_does_not_veto_rescuing_deep_failures<br>single_act_guard_allows_a_one_floor_shift_and_a_real_improvement |
| [rl/test_training_contract.py](rl/test_training_contract.py) | 3 | cpu_runtime_contract_does_not_initialize_cuda_libraries<br>local_source_digest_is_line_ending_stable |
| [rl/test_training_smoke.py](rl/test_training_smoke.py) | 19 | training_entrypoint_binds_evaluate_function_after_submodule_import<br>auto_resume_permits_only_safe_runtime_rebind |
| [rl/test_workers.py](rl/test_workers.py) | 9 | worker_pool_fails_fast_when_worker_exits_without_a_response<br>sharded_reset_many_preserves_requested_index_and_seed_order |

## runtime

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [runtime/test_inspector.py](runtime/test_inspector.py) | 12 | card_reward_presentation_exposes_composite_ui_action<br>score_is_complete_and_does_not_commit_memory |
| [runtime/test_live_setup.py](runtime/test_live_setup.py) | 3 | setup_finds_workshop_mods_and_exported_model<br>game_detection_uses_steam_libraryfolders |
| [runtime/test_policy_runtime.py](runtime/test_policy_runtime.py) | 13 | policy_artifact_round_trip_is_strict_and_standalone<br>boundary_id_is_candidate_order_independent |

## simulator

| 文件 | 测试函数数 | 检查入口（前两个测试名） |
| --- | ---: | --- |
| [simulator/test_a20_late_act_parity.py](simulator/test_a20_late_act_parity.py) | 10 | reptomancer_reused_daggers_checkpoint_round_trip<br>collector_mega_debuff_uses_stock_ascension_duration |
| [simulator/test_a20_scope_boundaries.py](simulator/test_a20_scope_boundaries.py) | 4 | note_for_yourself_is_absent_from_a20_generation_pool<br>prismatic_shard_remains_in_a20_shop_rng_pool |
| [simulator/test_bottled_cards.py](simulator/test_bottled_cards.py) | 2 | bottle_card_projects_into_deck_combat_and_checkpoint<br>bottle_marker_changes_encoding_without_revealing_internal_card_id |
| [simulator/test_combat_upgrade_costs.py](simulator/test_combat_upgrade_costs.py) | 2 | blood_for_blood_upgrade_preserves_damage_discount<br>upgrade_keeps_a_madness_card_free_for_this_turn |
| [simulator/test_content_execution.py](simulator/test_content_execution.py) | 6 | every_scoped_playable_card_enters_the_native_action_pipeline<br>every_scoped_status_and_curse_has_the_stock_playability_path |
| [simulator/test_event_outcomes.py](simulator/test_event_outcomes.py) | 3 | designer_random_upgrade_costs_gold_like_selected_upgrade<br>dead_adventurer_third_safe_search_finishes_without_extra_fight |
| [simulator/test_fullrun_card_choices.py](simulator/test_fullrun_card_choices.py) | 6 | fullrun_forethought_plus_can_select_any_subset<br>fullrun_sacred_bark_liquid_memories_retrieves_two_chosen_cards |
| [simulator/test_fullrun_structure.py](simulator/test_fullrun_structure.py) | 3 | act_horizon_ends_at_boss_defeat_before_reward_choices<br>non_heart_fullrun_hides_key_choices_and_stops_after_act_three |
| [simulator/test_native_mechanisms.py](simulator/test_native_mechanisms.py) | 25 | nilrys_codex_exposes_its_skip_choice_control<br>original_compatible_rng_is_seeded_and_advances_exactly |
| [simulator/test_relic_availability.py](simulator/test_relic_availability.py) | 5 | newly_acquired_lizard_tail_revives_once_and_stays_spent_after_restore<br>relic_sentinels_change_encoded_observation |
| [simulator/test_simulator_backend.py](simulator/test_simulator_backend.py) | 22 | native_full_run_reaches_a_canonical_decision<br>signed_official_seed_matches_same_unsigned_native_bits |
| [simulator/test_transform_selection.py](simulator/test_transform_selection.py) | 2 | transform_selection_matches_stock_filtered_list<br>shield_gremlin_block_targets_match_stock_boundaries |
