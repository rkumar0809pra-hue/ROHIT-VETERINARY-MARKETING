with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

# 1. Add selectedPostId state right after selectedSortOption
old_decl = '    var selectedSortOption by remember { mutableStateOf("NEWEST") }'
new_decl = '''    var selectedSortOption by remember { mutableStateOf("NEWEST") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    val previewMode by viewModel.previewDeviceMode.collectAsState()
    val layoutInfo = com.example.ui.util.rememberScreenLayoutInfo(previewMode)'''

if old_decl in content:
    content = content.replace(old_decl, new_decl)

# 2. Add selectedPost calculation after sortedPosts declaration
sorted_pos = content.find('    val sortedPosts = filteredPosts.sortedWith { a, b ->')
end_sorted = content.find('    }\n\n    LazyColumn(', sorted_pos)
if end_sorted != -1:
    inject_pos = end_sorted + len('    }\n')
    selected_code = '''
    val selectedPost = sortedPosts.firstOrNull { it.id == selectedPostId } ?: sortedPosts.firstOrNull()
'''
    content = content[:inject_pos] + selected_code + content[inject_pos:]

# 3. Replace the list items in LazyColumn
old_items = '''        } else {
            items(sortedPosts) { post ->
                LibraryPostItemCard(
                    post = post,
                    currentRole = currentRole,
                    onApprove = { viewModel.approvePost(post.id) },
                    onRejectPrompt = {
                        postToReject = post
                        rejectReasonText = ""
                    },
                    onSchedulePrompt = { postToSchedule = post },
                    onPublish = { viewModel.publishPost(post.id, context) },
                    onDuplicate = {
                        viewModel.duplicatePost(post)
                        Toast.makeText(context, "Duplicated as new Draft!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        viewModel.deletePost(post.id)
                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                    },
                    onCopy = {
                        copyToClipboard(context, "${post.contentText}\\n\\n${post.hashtags}")
                    }
                )
            }
        }'''

new_items = '''        } else if (layoutInfo.isExpanded) {
            // Desktop Two-Pane Master-Detail Layout (List on left, preview detail on right)
            item {
                com.example.ui.components.ResponsiveTwoPaneLayout(
                    isWideScreen = true,
                    primaryWeight = 0.45f,
                    secondaryWeight = 0.55f,
                    primaryPane = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sortedPosts.forEach { post ->
                                LibraryPostListItem(
                                    post = post,
                                    isSelected = post.id == selectedPost?.id,
                                    onClick = { selectedPostId = post.id },
                                    onCopy = { copyToClipboard(context, "${post.contentText}\\n\\n${post.hashtags}") }
                                )
                            }
                        }
                    },
                    secondaryPane = {
                        LibraryPostDetailPanel(
                            post = selectedPost,
                            currentRole = currentRole,
                            viewModel = viewModel,
                            context = context,
                            onApprove = { viewModel.approvePost(it.id) },
                            onRejectPrompt = {
                                postToReject = it
                                rejectReasonText = ""
                            },
                            onSchedulePrompt = { postToSchedule = it },
                            onPublish = { viewModel.publishPost(it.id, context) },
                            onDuplicate = {
                                viewModel.duplicatePost(it)
                                Toast.makeText(context, "Duplicated as new Draft!", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deletePost(it.id)
                                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        } else {
            // Mobile and Tablet single list with expandable cards
            items(sortedPosts) { post ->
                LibraryPostItemCard(
                    post = post,
                    currentRole = currentRole,
                    onApprove = { viewModel.approvePost(post.id) },
                    onRejectPrompt = {
                        postToReject = post
                        rejectReasonText = ""
                    },
                    onSchedulePrompt = { postToSchedule = post },
                    onPublish = { viewModel.publishPost(post.id, context) },
                    onDuplicate = {
                        viewModel.duplicatePost(post)
                        Toast.makeText(context, "Duplicated as new Draft!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        viewModel.deletePost(post.id)
                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                    },
                    onCopy = {
                        copyToClipboard(context, "${post.contentText}\\n\\n${post.hashtags}")
                    }
                )
            }
        }'''

if old_items in content:
    content = content.replace(old_items, new_items)
    with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
        f.write(content)
    print("SUCCESS")
else:
    print("FAILED TO MATCH old_items")
