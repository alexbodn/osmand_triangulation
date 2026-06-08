import re

with open('/tmp/HomeFragment.kt', 'r') as f:
    content = f.read()

content = content.replace('handleIntent(intent)', 'handleIntent(requireActivity().intent)')
content = content.replace('handleIntent(requireActivity().intent)', 'handleIntent(requireActivity().intent)', 1)

content = content.replace('requireActivity().intent = Intent()', 'requireActivity().intent.replaceExtras(Bundle())')

content = content.replace('val view = layoutInflater.inflate(R.layout.item_point, llPointsContainer, false)', 'val itemView = layoutInflater.inflate(R.layout.item_point, llPointsContainer, false)')
content = content.replace('val tvAzimuth = view.view.findViewById<TextView>(R.id.tvPointAzimuth)', 'val tvAzimuth = itemView.findViewById<TextView>(R.id.tvPointAzimuth)')
content = content.replace('val btnView = view.view.findViewById<Button>(R.id.btnView)', 'val btnView = itemView.findViewById<Button>(R.id.btnView)')
content = content.replace('val btnDelete = view.view.findViewById<Button>(R.id.btnDelete)', 'val btnDelete = itemView.findViewById<Button>(R.id.btnDelete)')
content = content.replace('llPointsContainer.addView(view)', 'llPointsContainer.addView(itemView)')

with open('/tmp/HomeFragment.kt', 'w') as f:
    f.write(content)
